package Server;

import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Packets.Packet;
import Packets.Server.ServerJobResultPacket;

public class SharedState {

    private final long memoryLimit;
    private long memoryUsed;

    private ReadWriteLock ljobs;
    private Queue<Job> jobs;

    private Condition hasJobs;
    private Condition hasMemory;

    private ReentrantLock lresults;

    private final int MAX_WORKERS = 10;
    private final int maxTimesWaited = 5;
    private int nBlocking;
    private int nWaiting;

    private ReadWriteLock lt;
    private Thread[] workerThreads;
    private ServerWorker[] workers;

    private ReadWriteLock lc;
    private Map<Long, Connection> connections;

    public SharedState(long memoryLimit) {
        this.memoryUsed = 0;
        this.memoryLimit = memoryLimit;
        this.nBlocking = 0;
        this.nWaiting = 0;

        this.jobs = new ArrayDeque<>();
        this.ljobs = new ReentrantReadWriteLock();
        this.hasJobs = this.ljobs.writeLock().newCondition();
        this.hasMemory = this.ljobs.writeLock().newCondition();

        this.lresults = new ReentrantLock();
        this.lc = new ReentrantReadWriteLock();
        this.connections = new HashMap<>();

        this.lt = new ReentrantReadWriteLock();
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

    public void newServerWorker() {
        try {
            this.lt.writeLock().lock();
            int nWorkers = this.workerThreads.length;
            if (nWorkers < this.MAX_WORKERS) {
                this.workerThreads[nWorkers] = new Thread(new ServerWorker(this));
                this.workerThreads[nWorkers].start();
            }
        } finally {
            this.lt.writeLock().unlock();
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

    public void enqueueJob(Job job) {

        if (job.getRequiredMemory() > this.memoryLimit) {
            Packet packet = new ServerJobResultPacket(job.getId(), "Job requires more memory than the server can provide.");
            this.sendJobResult(job.getThreadId(), job.getRequiredMemory(), packet);
            return;
        }

        try {
            this.ljobs.writeLock().lock();
            this.jobs.add(job);
            this.hasJobs.signal();
        } finally {
            this.ljobs.writeLock().unlock();
        }
    }

    public Job dequeueJob() {
        try {
            this.ljobs.writeLock().lock();

            this.nWaiting += 1;

            while (jobs.isEmpty())
                this.hasJobs.await();

            Job job = this.jobs.poll();
            
            int timesWaited = 0;
            while (job.getRequiredMemory() + this.memoryUsed > this.memoryLimit 
            || (this.nBlocking > 0 && timesWaited < this.maxTimesWaited)) {
                this.hasMemory.await();
                timesWaited += 1;
                if (timesWaited == this.maxTimesWaited)
                    this.nBlocking += 1;
            }

            this.nWaiting -= 1;

            if (timesWaited > this.maxTimesWaited) {
                this.nBlocking -= 1;
            }
            
            this.memoryUsed += job.getRequiredMemory();

            return job;
        } catch (InterruptedException e) {
            return null;
        } finally {
            this.ljobs.writeLock().unlock();
        }
    }

    public void sendJobResult(long id, long requiredMemory, Packet packet) {
        try {
            this.lresults.lock();
            Connection connection = this.connections.get(id);
            if (connection != null) {
                try {
                    this.ljobs.writeLock().lock();
                    this.memoryUsed -= requiredMemory;
                    this.hasMemory.signalAll();
                } finally {
                    this.ljobs.writeLock().unlock();
                }
                connection.addPacketToQueue(packet);
                System.out.println("Sent job result to client.");
            } else {
                System.out.println("Client disconnected before receiving job result.");
            }
        } finally {
            this.lresults.unlock();
        }
    }

    public long getMaxMemory() {
        return this.memoryLimit;
    }

    public long getAvailableMemory() {
        try {
            this.ljobs.readLock().lock();
            return this.memoryLimit - this.memoryUsed;
        } finally {
            this.ljobs.readLock().unlock();
        }
    }

    public int getQueueSize() {
        try {
            this.ljobs.readLock().lock();
            return this.jobs.size();
        } finally {
            this.ljobs.readLock().unlock();
        }
    }

    public int getNConnections() {
        try {
            this.lc.readLock().lock();
            return this.connections.size();
        } finally {
            this.lc.readLock().unlock();
        }
    }

    public int getNWorkers() {
        try {
            this.lt.readLock().lock();
            return this.workerThreads.length;
        } finally {
            this.lt.readLock().unlock();
        }
    }

    public int getNWorkersWaiting() {
        try {
            this.ljobs.readLock().lock();
            return this.nWaiting;
        } finally {
            this.ljobs.readLock().unlock();
        }
    }
}