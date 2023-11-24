package Server;

import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Packets.Packet;

public class SharedState {

    private final long memoryLimit;
    private long memoryUsed;

    private ReentrantLock ljobs;
    private Queue<Job> jobs;

    private Condition hasJobs;
    private Condition hasMemory;

    private ReentrantLock lresults;

    private final int MAX_WORKERS = 10;
    private final int INITIAL_WORKERS = 5;

    private ReentrantLock lt;
    private Thread[] workerThreads;
    private ServerWorker[] workers;

    private ReentrantLock lc;
    private Map<Long, Connection> connections;

    public SharedState(long memoryLimit) {
        this.memoryUsed = 0;
        this.memoryLimit = memoryLimit;

        this.jobs = new ArrayDeque<>();
        this.ljobs = new ReentrantLock();
        this.hasJobs = this.ljobs.newCondition();
        this.hasMemory = this.ljobs.newCondition();

        this.lresults = new ReentrantLock();
        this.lc = new ReentrantLock();
        this.connections = new HashMap<>();

        this.lt = new ReentrantLock();
        this.workers = new ServerWorker[this.MAX_WORKERS];
        this.workerThreads = new Thread[this.MAX_WORKERS];
        this.runWorkers();
    }

    private void runWorkers() {
        for (int i = 0; i < this.INITIAL_WORKERS; i++) {
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

    public void enqueueJob(Job job) {
        // TODO: handle this case -> memory required > memory limit
        try {
            this.ljobs.lock();

            while (this.memoryUsed + job.getRequiredMemory() > this.memoryLimit)
                this.hasMemory.await();

            this.memoryUsed += job.getRequiredMemory();

            this.jobs.add(job);
            this.hasJobs.signal();
        } catch (InterruptedException e) {
            // job lost, oh no
        } finally {
            this.ljobs.unlock();
        }
    }

    public Job dequeueJob() {
        try {
            this.ljobs.lock();

            while (jobs.isEmpty())
                this.hasJobs.await();

            Job job = this.jobs.poll();
            this.memoryUsed -= job.getRequiredMemory();
            this.hasMemory.signalAll();
            return job;
        } catch (InterruptedException e) {
            return null;
        } finally {
            this.ljobs.unlock();
        }
    }

    public void sendJobResult(long id, Packet packet) {
        try {
            this.lresults.lock();
            Connection connection = this.connections.get(id);
            if (connection != null) {
                connection.addPacketToQueue(packet);
                System.out.println("Sent job result to client.");
            } else {
                System.out.println("Client disconnected before receiving job result.");
            }
        } finally {
            this.lresults.unlock();
        }
    }
}