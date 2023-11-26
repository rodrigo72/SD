package Worker;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;
import Packets.Worker.*;
import Packets.Packet;
import Packets.Server.ServerJobPacket;
import Packets.Server.ServerPacketDeserializer;
import Packets.Server.ServerPacketType;
import sd23.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class Worker {
    private long nextID;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Socket socket;
    private final WorkerPacketSerializer serializer;
    private final ServerPacketDeserializer deserializer;
    private final long maxMemory;
    private boolean running;
    private Thread thread;
    private Queue<ServerJobPacket> jobs;
    private ReentrantLock ljobs;
    private Thread[] workerThreads;
    private int nWorkerThreads;
    private Condition hasJobs;
    private Condition hasMemory; // for safety, the server only sends jobs if the worker has memory available for it (!)
    private Condition hasBlocking;
    private ReentrantLock lsend;
    private long memoryUsed;
    private boolean blocking;
    private final int maxTimesWaited;

    public Worker(String address, int port, long maxMemory, int nWorkerThreads) throws IOException {
        this.socket = new Socket(address, port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.nextID = 0L;
        this.maxMemory = maxMemory;
        this.running = true;
        this.serializer = new WorkerPacketSerializer();
        this.deserializer = new ServerPacketDeserializer();
        this.hook(socket);
        this.thread = null;
        this.jobs = new LinkedList<>();
        this.ljobs = new ReentrantLock();
        this.hasJobs = this.ljobs.newCondition();
        this.hasMemory = this.ljobs.newCondition();
        this.hasBlocking = this.ljobs.newCondition();
        this.workerThreads = new Thread[nWorkerThreads];
        this.nWorkerThreads = nWorkerThreads;
        this.lsend = new ReentrantLock();
        this.memoryUsed = 0L;
        this.maxTimesWaited = 3;
    }

    private void hook(Socket socket) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Worker: " + e.getMessage());
                }
            }
        });
    }

    public void runAllWorkerThreads() {
        for (int i = 0; i < this.nWorkerThreads; i++)
            this.workerThreads[i] = new Thread(() -> this.runWorker());

        for (int i = 0; i < this.nWorkerThreads; i++)
            this.workerThreads[i].start();

    }

    private long getNextID() {
        return this.nextID++;
    }

    public long sendConnectionPacket() throws IOException {
        long id = this.getNextID();
        WorkerConnectionPacket packet = new WorkerConnectionPacket(id, this.maxMemory);
        serializer.serialize(out, packet);
        return id;
    }

    public long sendJobResultPacket(String clientName, byte[] data) throws IOException {
        long id = this.getNextID();
        WorkerJobResultPacket packet = new WorkerJobResultPacket(id, data, clientName);
        serializer.serialize(out, packet);
        return id;
    }

    public long sendDisconnectionPacket() throws IOException {
        long id = this.getNextID();
        WorkerDisconnectionPacket packet = new WorkerDisconnectionPacket(id);
        serializer.serialize(out, packet);
        return id;
    }

    public void stop() throws IOException {
        this.running = false;
        this.in.close();
        try {
            this.ljobs.lock();
            this.hasJobs.signalAll();
        } finally {
            this.ljobs.unlock();
        }
    }

    public void receive() {
        if (this.thread == null) {

            try {
                this.sendConnectionPacket();
            } catch (IOException e) {
                return;
            }

            this.runAllWorkerThreads();

            this.thread = new Thread(() -> {
                while(this.running) {

                    Packet p = null;
                    try {
                        p = this.deserializer.deserialize(this.in);
                    } catch (IOException e) {
                        this.running = false;
                        break;
                    }

                    ServerPacketType type = (ServerPacketType) p.getType();
                    
                    switch(type) {
                        case JOB -> {
                            ServerJobPacket packet = (ServerJobPacket) p;

                            System.out.println("JOB packet received");

                            try {
                                this.ljobs.lock();
                                this.jobs.add(packet);
                                this.hasJobs.signal();
                            } finally {
                                this.ljobs.unlock();
                            }

                        }
                        default -> {
                            // nothing
                        }
                    }
                }
            });
            this.thread.start();
        }
    }

    private void runWorker() {
        while (this.running) {
            ServerJobPacket packet;
            try {
                this.ljobs.lock();

                while(this.jobs.isEmpty() && this.running) {
                    this.hasJobs.await();
                }

                packet = this.jobs.poll();

                if (packet == null)
                    continue;

                int timesWaited = 0;
                boolean blocking = false;
                long requiredMemory = packet.getRequiredMemory();

                // This loop is not necessary because the server only sends JobPackets when the worker has enough memory to handle them.
                // So, in the current implementation, a scenario where memory is insufficient to execute a job is unlikely.
                // However, if the server logic changes in the future, maintaining this check ensures the worker continues to function correctly.
                // (will also prevent starvation with the maxTimesWaited and blocking condition)

                while (requiredMemory + this.memoryUsed > this.maxMemory && this.running && (this.blocking && !blocking)) {
                    if (this.blocking) {
                        this.hasBlocking.await();
                    } else {
                        this.hasMemory.await();
                    }

                    timesWaited += 1;

                    if (!this.blocking && timesWaited > this.maxTimesWaited) {
                        blocking = true;
                        this.blocking = true;
                    }
                }

                if (blocking)   
                    this.blocking = false;

                this.memoryUsed += requiredMemory;

            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                packet = null;
            } finally {
                this.ljobs.unlock();
            }

            WorkerJobResultPacket resultPacket = null;
            try {
                byte[] output = JobFunction.execute(packet.getData());
                resultPacket = new WorkerJobResultPacket(packet.getId(), output, packet.getClientName());
            } catch (JobFunctionException e) {
                resultPacket = new WorkerJobResultPacket(packet.getId(), e.getMessage(), packet.getClientName());
            }
            
            System.out.println(resultPacket.toString());

            try {
                this.lsend.lock();
                this.serializer.serialize(out, resultPacket);
                System.out.println("Result packet sent");

                try {
                    this.ljobs.lock();
                    this.memoryUsed -= packet.getRequiredMemory();
                    if (this.blocking) {
                        this.hasBlocking.signal();
                    } else {
                        this.hasMemory.signal();
                    }
                } finally {
                    this.ljobs.unlock();
                }

            } catch (IOException e) {
                this.running = false;
            } finally {
                this.lsend.unlock();
            }

        }
    }
    
}
