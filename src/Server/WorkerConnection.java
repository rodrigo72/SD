package Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import Packets.Packet;
import Packets.Server.*;
import Packets.Worker.WorkerConnectionPacket;
import Packets.Worker.WorkerJobResultPacket;
import Packets.Worker.WorkerPacketDeserializer;
import Packets.Worker.WorkerPacketType;

import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.ArrayDeque;

public class WorkerConnection extends Connection {

    private int nThreads;
    private long maxMemory;
    private long memoryUsed;
    private Thread workThread;
    private ReentrantLock ljobs;
    private Condition hasJobs;
    private Map<String, Map<Long, Long>> jobsRequiredMemory;
    private boolean working;
    private boolean running;
    private Queue<Job> jobs;

    public WorkerConnection(SharedState sharedState, Socket socket, boolean debug) {
        super(sharedState, socket, debug);

        this.nThreads = 0;
        this.maxMemory = -1;
        this.working = false;
        this.running = false;
        this.setDeserializer(new WorkerPacketDeserializer());
        this.setSerializer(new ServerPacketSerializer());
        this.startOutputThread();
        this.workThread = new Thread(() -> this.work());

        this.ljobs = new ReentrantLock();
        this.hasJobs = this.ljobs.newCondition();

        this.jobsRequiredMemory = new HashMap<>();
        this.jobs = new ArrayDeque<>();
    }

    public void enqueueJob(Job job) {
        try {
            this.ljobs.lock();
            this.jobs.add(job);
            this.hasJobs.signal();
        } finally {
            this.ljobs.unlock();
        }
    }

    @Override
    public void run() {
        try {
            this.running = true;
            this.threadId = Thread.currentThread().getId();
            while (this.running) {
                
                Packet p = this.deserialize();
                long id = p.getId();
                WorkerPacketType type = (WorkerPacketType) p.getType();

                if (this.debug)
                    System.out.println("Received packet of type " + type);

                switch (type) {
                    case CONNECTION -> {
                        this.handleConnectionPacket(p, id);
                    }
                    case JOB_RESULT -> {
                        this.handleJobResultPackeet(p, id);
                    }
                    case DISCONNECTION -> {
                        this.sharedState.removeFromLimits(this.maxMemory, this.memoryUsed, this.threadId);
                        this.running = false;
                        try {
                            this.ljobs.lock();
                            this.hasJobs.signal();
                        } finally {
                            this.ljobs.unlock();
                        }
                        this.sharedState.removeWorkerConnection(this.threadId);
                    }
                    default -> {
                        // do nothing
                    }
                }
            }
        } catch (IOException e) {
            if (this.debug)
                System.out.println("Worker connection: " + e.getMessage());

            this.sharedState.removeFromLimits(this.maxMemory, this.memoryUsed, this.threadId);
            this.sharedState.removeWorkerConnection(this.threadId);

            try {
                this.l.lock();
                this.outputThread.interrupt();
                this.packetsToSend.notEmpty.signal();
            } finally {
                this.l.unlock();
            }
        }
    }

    public void handleConnectionPacket(Packet p, long id) {

        if (this.debug)
            System.out.println("Received worker connection");

        WorkerConnectionPacket packet = (WorkerConnectionPacket) p;
        this.maxMemory = packet.getMaxMemory();
        this.nThreads = packet.getNThreads();
        this.sharedState.addToLimits(this.maxMemory, this.threadId, this.nThreads);
        if (!this.working) {
            this.working = true;
            this.workThread.start();
        }
    } 

    public void handleJobResultPackeet(Packet p, long id) {

        if (this.debug)
            System.out.println("Received job result packet");

        if (!this.working)
            return;

        WorkerJobResultPacket packet = (WorkerJobResultPacket) p;
        String clientName = packet.getClientName();
        
        Map<Long, Long> innerMap = this.jobsRequiredMemory.get(clientName);
        if (innerMap != null) {
            Long requiredMemory = innerMap.remove(id);
            if (requiredMemory != null) {

                Packet packet2 = null;
                if (packet.getStatus() == WorkerJobResultPacket.ResultStatus.SUCCESS)
                    packet2 = new ServerJobResultPacket(id, packet.getData());
                else
                    packet2 = new ServerJobResultPacket(id, packet.getErrorMessage());
                
                if (this.debug)
                    System.out.println("Sending job result packet");

                this.sharedState.sendJobResult(clientName, packet2, requiredMemory, this.threadId);
            }            
        }

    }

    public void work() {
        while (this.running) {
            try {
                this.ljobs.lock();

                while (this.jobs.isEmpty())
                    this.hasJobs.await();

                Job job = this.jobs.poll();
                
                String clientName = job.getClientName();
                long packetId = job.getId();
                long requiredMemory = job.getRequiredMemory();

                ServerJobPacket packet = new ServerJobPacket(
                    packetId, clientName, requiredMemory, job.getData()
                );

                this.jobsRequiredMemory
                    .computeIfAbsent(clientName, k -> new HashMap<>())
                    .put(packetId, requiredMemory);

                if (this.debug)
                    System.out.println("Worker connection added packet to queue.");

                this.addPacketToQueue(packet);

            } catch (InterruptedException e) {
                continue;
            } finally {
                this.ljobs.unlock();
            }
        }
    }
}
