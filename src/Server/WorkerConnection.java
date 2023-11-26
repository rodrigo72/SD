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

public class WorkerConnection extends Connection {

    private long maxMemory;
    private long memoryUsed;
    private Thread workThread;
    private ReentrantLock lmem;
    private Condition hasMemory;
    private Map<String, Map<Long, Long>> jobsRequiredMemory;
    private boolean working;
    private boolean running;

    public WorkerConnection(SharedState sharedState, Socket socket, boolean debug) {
        super(sharedState, socket, debug);

        this.maxMemory = -1;
        this.memoryUsed = 0L;
        this.working = false;
        this.running = false;
        this.setDeserializer(new WorkerPacketDeserializer());
        this.setSerializer(new ServerPacketSerializer());
        this.startOutputThread();
        this.workThread = new Thread(() -> this.work());

        this.lmem = new ReentrantLock();
        this.hasMemory = this.lmem.newCondition();

        this.jobsRequiredMemory = new HashMap<>();
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
                        this.sharedState.removeFromLimits(this.maxMemory);
                        this.running = false;
                        try {
                            this.lmem.lock();
                            this.hasMemory.signal();
                        } finally {
                            this.lmem.unlock();
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

            this.sharedState.removeFromLimits(this.maxMemory);
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
        this.sharedState.addToLimits(this.maxMemory);
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
                try {
                    this.lmem.lock();
                    this.memoryUsed -= requiredMemory;
                    this.hasMemory.signal();
                } finally {
                    this.lmem.unlock();
                }

                Packet packet2 = null;
                if (packet.getStatus() == WorkerJobResultPacket.ResultStatus.SUCCESS)
                    packet2 = new ServerJobResultPacket(id, packet.getData());
                else
                    packet2 = new ServerJobResultPacket(id, packet.getErrorMessage());

                this.sharedState.sendJobResult(clientName, packet2);
            }            
        }

    }

    public void work() {
        while (this.running) {
            Job job = this.sharedState.dequeueJob(this.maxMemory);
            if (job == null)
                continue;

            try {
                this.lmem.lock();
                
                while (job.getRequiredMemory() + this.memoryUsed > this.maxMemory && this.running)
                    this.hasMemory.await();

                if (!this.running) {
                    this.sharedState.enqueueJob(job);
                    return;
                }

                this.memoryUsed += job.getRequiredMemory();

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
                this.lmem.unlock();
            }
        }
    }
}
