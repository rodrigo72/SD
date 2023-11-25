package Server;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Packets.Packet;
import Packets.Server.ServerJobResultPacket;
import Utils.MeasureSelectorQueue;

public class SharedState {

    private final long memoryLimit;
    private long memoryUsed;

    private ReentrantLock ljobs;
    private MeasureSelectorQueue<Job> jobs;

    private Condition hasJobs;
    private Condition hasMemory;
    private Condition hasBlocking;
    private Condition notFull;

    private final int MAX_WORKERS = 3;
    private final int maxTimesWaited = 2;
    private int nWaiting;
    private boolean blocking;

    private Thread[] workerThreads;
    private ServerWorker[] workers;

    private ReentrantReadWriteLock lc;
    private Map<Long, Connection> connections;
    private Map<String, Long> clientThreads;

    public SharedState(long memoryLimit) {
        this.memoryUsed = 0;
        this.memoryLimit = memoryLimit;
        this.nWaiting = 0;
        this.blocking = false;

        this.jobs = new MeasureSelectorQueue<>(1000);
        this.ljobs = new ReentrantLock();
        this.hasJobs = this.ljobs.newCondition();
        this.hasMemory = this.ljobs.newCondition();
        this.hasBlocking = this.ljobs.newCondition();
        this.notFull = this.ljobs.newCondition();

        this.lc = new ReentrantReadWriteLock();
        this.connections = new HashMap<>();
        this.clientThreads = new HashMap<>();

        this.workers = new ServerWorker[this.MAX_WORKERS];
        this.workerThreads = new Thread[this.MAX_WORKERS];
        this.runMaxWorkers();
    }

    private void runMaxWorkers() {
        for (int i = 0; i < this.MAX_WORKERS; i++) {
            this.workers[i] = new ServerWorker(this);
            this.workerThreads[i] = new Thread(this.workers[i]);
            this.workerThreads[i].start();
        }
    }

    public void stopWorkers() {
        for (int i = 0; i < this.workerThreads.length; i++) {
            if (this.workerThreads[i] != null)
                this.workers[i].stop();
                this.workerThreads[i].interrupt();
        }
    }

    public void addConnection(Connection connection, long threadID) {
        try {
            this.lc.writeLock().lock();
            this.connections.put(threadID, connection);
        } finally {
            this.lc.writeLock().unlock();
        }
    }

    public void removeConnection(long threadID) {
        try {
            this.lc.writeLock().lock();
            this.connections.remove(threadID);
        } finally {
            this.lc.writeLock().unlock();
        }
    }

    public void addClientThread(String clientID, long threadID) {
        try {
            this.lc.writeLock().lock();
            this.clientThreads.put(clientID, threadID);
        } finally {
            this.lc.writeLock().unlock();
        }
    }

    public void removeClientThread(String clientID) {
        try {
            this.lc.writeLock().lock();
            this.clientThreads.remove(clientID);
        } finally {
            this.lc.writeLock().unlock();
        }
    }

    public void enqueueJob(Job job) {

        if (job.getRequiredMemory() > this.memoryLimit) {
            Packet packet = new ServerJobResultPacket(job.getId(), "Job requires more memory than the server can provide.");
            this.sendJobResult(job.getClientName(), job.getRequiredMemory(), packet);
            return;
        }

        try {
            this.ljobs.lock();
            
            while (this.jobs.isFull())
                this.notFull.await();

            this.jobs.add(job);
            this.hasJobs.signal();
        } catch (InterruptedException e) {
            return;
        } finally {
            this.ljobs.unlock();
        }
    }

    public Job dequeueJob() {
        try {
            this.ljobs.lock();

            this.nWaiting += 1;

            while (jobs.isEmpty())
                this.hasJobs.await();

            Job job = this.jobs.poll(this.memoryLimit);
            this.notFull.signal();
            
            int timesWaited = 0;
            boolean blocking = false;

            while (job.getRequiredMemory() + this.memoryUsed > this.memoryLimit || (this.blocking && !blocking)) {
                
                if (blocking) {
                    this.hasBlocking.await();
                } else {
                    this.hasMemory.await();
                }

                timesWaited += 1;
                if (!this.blocking && timesWaited >= this.maxTimesWaited) {
                    blocking = true;
                    this.blocking = true;
                    System.out.println(" ! BLOCKING ! --> " + job.toString());
                }
            }

            this.nWaiting -= 1;

            if (blocking)
                this.blocking = false;
            
            this.memoryUsed += job.getRequiredMemory();

            return job;
        } catch (InterruptedException e) {
            return null;
        } finally {
            this.ljobs.unlock();
        }
    }

    public void sendJobResult(String clientName, long requiredMemory, Packet packet) {
        try {
            this.lc.readLock().lock();

            Long threadId = this.clientThreads.get(clientName);
            if (threadId != null) {
                Connection connection = this.connections.get(threadId);
                if (connection != null) {
                    try {
                        this.ljobs.lock();
                        this.memoryUsed -= requiredMemory;
                        if (this.blocking) {
                            this.hasBlocking.signal();
                        } else {
                            this.hasMemory.signalAll();
                        }
                    } finally {
                        this.ljobs.unlock();
                    }
                    connection.addPacketToQueue(packet);
                    System.out.println("Sent job result to client.");
                } else {
                    System.out.println("Client disconnected before receiving job result.");
                }
            } else {
                System.out.println("Client disconnected before receiving job result.");
            }
        } finally {
            this.lc.readLock().unlock();
        }
    }

    public long getMaxMemory() {
        return this.memoryLimit;
    }

    public long getAvailableMemory() {
        return this.memoryLimit - this.memoryUsed;
    }

    public int getQueueSize() {
        return this.jobs.size();
    }

    public int getNConnections() {
        return this.connections.size();
    }

    public int getNWorkers() {
        return this.workerThreads.length;
    }

    public int getNWorkersWaiting() {
        return this.nWaiting;
    }
}