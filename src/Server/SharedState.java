package Server;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

    private ReentrantLock lresults;

    private final int MAX_WORKERS = 10;
    private final int maxTimesWaited = 5;
    private int nBlocking;
    private int nWaiting;

    private ReentrantLock lt;
    private Thread[] workerThreads;
    private ServerWorker[] workers;

    private ReentrantLock lc;
    private Map<Long, Connection> connections;

    public SharedState(long memoryLimit) {
        this.memoryUsed = 0;
        this.memoryLimit = memoryLimit;
        this.nBlocking = 0;
        this.nWaiting = 0;

        this.jobs = new MeasureSelectorQueue<>();
        this.ljobs = new ReentrantLock();
        this.hasJobs = this.ljobs.newCondition();
        this.hasMemory = this.ljobs.newCondition();

        this.lresults = new ReentrantLock();
        this.lc = new ReentrantLock();
        this.connections = new HashMap<>();

        this.lt = new ReentrantLock();
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
            this.lt.lock();
            int nWorkers = this.workerThreads.length;
            if (nWorkers < this.MAX_WORKERS) {
                this.workerThreads[nWorkers] = new Thread(new ServerWorker(this));
                this.workerThreads[nWorkers].start();
            }
        } finally {
            this.lt.unlock();
        }
    }

    public void addConnection(Connection connection, long threadID) {
        try {
            this.lc.lock();
            this.connections.put(threadID, connection);
        } finally {
            this.lc.unlock();
        }
    }

    public void removeConnection(long threadID) {
        try {
            this.lc.lock();
            this.connections.remove(threadID);
        } finally {
            this.lc.unlock();
        }
    }

    public void enqueueJob(Job job) {

        if (job.getRequiredMemory() > this.memoryLimit) {
            Packet packet = new ServerJobResultPacket(job.getId(), "Job requires more memory than the server can provide.");
            this.sendJobResult(job.getThreadId(), job.getRequiredMemory(), packet);
            return;
        }

        try {
            this.ljobs.lock();
            this.jobs.add(job);
            this.hasJobs.signal();
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
            this.ljobs.unlock();
        }
    }

    public void sendJobResult(long id, long requiredMemory, Packet packet) {
        try {
            this.lresults.lock();
            Connection connection = this.connections.get(id);
            if (connection != null) {
                try {
                    this.ljobs.lock();
                    this.memoryUsed -= requiredMemory;
                    this.hasMemory.signalAll();
                } finally {
                    this.ljobs.unlock();
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