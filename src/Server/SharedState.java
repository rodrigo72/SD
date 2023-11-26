package Server;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Packets.Packet;
import Packets.Server.ServerJobResultPacket;
import Utils.MeasureSelectorQueue;

public class SharedState {

    private long memoryLimit;
    private long memoryUsed;

    private ReentrantLock ljobs;
    private MeasureSelectorQueue<Job> jobs;

    private Condition hasJobs;
    private Condition notFull;

    private int nWaiting;

    private ReentrantReadWriteLock lc;
    private Map<Long, ClientConnection> connections;
    private Map<String, Long> clientThreads;

    private ReentrantLock lwc;
    private Map<Long, WorkerConnection> workerConnections;

    private PriorityQueue<Long> maxHeap;

    public SharedState() {
        this.memoryUsed = 0;
        this.nWaiting = 0;

        this.jobs = new MeasureSelectorQueue<>(1000);
        this.ljobs = new ReentrantLock();
        this.hasJobs = this.ljobs.newCondition();
        this.notFull = this.ljobs.newCondition();

        this.lc = new ReentrantReadWriteLock();
        this.connections = new HashMap<>();
        this.clientThreads = new HashMap<>();

        this.lwc = new ReentrantLock();
        this.workerConnections = new HashMap<>();

        this.maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    }

    public void addWorkerConnection(WorkerConnection conn, long threadId) {
        try {
            this.lwc.lock();
            this.workerConnections.put(threadId, conn);
        } finally {
            this.lwc.unlock();
        }
    }

    public void removeWorkerConnection(long threadId) {
        try {
            this.lwc.lock();
            this.workerConnections.remove(threadId);
        } finally {
            this.lwc.unlock();
        }
    }

    public void addToLimits(long maxMemory) {
        try {
            this.ljobs.lock();

            this.maxHeap.add(maxMemory);
            this.memoryLimit = this.maxHeap.peek();

        } finally {
            this.ljobs.unlock();
        }
    }

    public void removeFromLimits(long maxMemory) {
        try {
            this.ljobs.lock();

            long newMax = -2;
            this.maxHeap.remove(maxMemory);
            if (this.maxHeap.isEmpty()) {
                this.memoryLimit = -1;
            } else {
                newMax = this.maxHeap.peek();
                this.memoryLimit = this.maxHeap.peek();
            }

            if (newMax < this.memoryLimit) {
                List<Job> jobsRemovedFromQueue = this.jobs.removeIfGreater(newMax);
                for (Job job : jobsRemovedFromQueue) {
                    Packet packet = new ServerJobResultPacket(job.getId(),
                        "Job requires more memory than the server can provide. (Worker disconnected).");
                    this.sendJobResult(job.getClientName(), packet);
                }
            }
        } finally {
            this.ljobs.unlock();
        }
    }


    public void addConnection(ClientConnection connection, long threadID) {
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

        try {
            this.ljobs.lock();

            if (job.getRequiredMemory() > this.memoryLimit) {
                Packet packet = new ServerJobResultPacket(job.getId(), "Job requires more memory than the server can provide.");
                this.sendJobResult(job.getClientName(), packet);
                return;
            }
            
            while (this.jobs.isFull())
                this.notFull.await();

            this.jobs.add(job);
            this.hasJobs.signalAll();
        } catch (InterruptedException e) {
            return;
        } finally {
            this.ljobs.unlock();
        }
    }

    public Job dequeueJob(long maxMemory) {
        try {
            this.ljobs.lock();

            this.nWaiting += 1;

            while (jobs.isEmpty(maxMemory))
                this.hasJobs.await();

            Job job = this.jobs.poll(maxMemory);
            this.notFull.signal();
            
            return job;
        } catch (InterruptedException e) {
            return null;
        } finally {
            this.ljobs.unlock();
        }
    }

    public void sendJobResult(String clientName, Packet packet) {
        try {
            this.lc.readLock().lock();

            Long threadId = this.clientThreads.get(clientName);
            if (threadId != null) {
                ClientConnection connection = this.connections.get(threadId);
                if (connection != null) {
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
        return this.workerConnections.size();
    }

    public int getNWorkersWaiting() {
        return this.nWaiting;
    }
}