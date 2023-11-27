package Server;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;
import Packets.Packet;
import Packets.Server.ServerJobResultPacket;

public class SharedState {

    private long memoryLimit;
    private long memoryUsed;
    private long totalMemory;
    private long queueCap;

    private ReentrantLock ljobs;
    private Condition hasMem;
    private int nWaiting;

    private ReentrantReadWriteLock lc;
    private Map<Long, ClientConnection> connections;
    private Map<String, Long> clientThreads;

    private ReentrantLock lwc;
    private Map<Long, WorkerConnection> workerConnections;

    private PriorityQueue<Long> maxHeap;
    private List<Entry> sortedEntries;

    private Map<Long, Entry> entryMap;

    private ReentrantLock lqueue;
    private Condition notEmpty;
    private Condition notFull;
    private Queue<Job> jobs;

    private Thread distributeJobsThread;
    private boolean running;

    private class Entry implements Comparable<Entry> {
        long availableMemory;
        long threadId;
        long availableThreads;

        public Entry(long availableMemory, long threadId, long availableThreads) {
            this.availableMemory = availableMemory;
            this.threadId = threadId;
            this.availableThreads = availableThreads;
        }

        public int compareTo(Entry e) {
            int result = Long.compare(this.availableMemory, e.availableMemory);
            if (result == 0)
                result = Long.compare(this.availableThreads, e.availableThreads);
                if (result == 0)
                    result = Long.compare(this.threadId, e.threadId);
            return result;
        }

        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            Entry e = (Entry) obj;

            return threadId == e.threadId;
        }
    }

    public SharedState() {

        this.memoryUsed = 0;
        this.nWaiting = 0;
        this.totalMemory = 0;
        this.queueCap = 100;

        this.ljobs = new ReentrantLock();
        this.hasMem = this.ljobs.newCondition();

        this.lqueue = new ReentrantLock();
        this.jobs = new LinkedList<>();
        this.notEmpty = this.lqueue.newCondition();
        this.notFull = this.lqueue.newCondition();

        this.lc = new ReentrantReadWriteLock();
        this.connections = new HashMap<>();
        this.clientThreads = new HashMap<>();

        this.lwc = new ReentrantLock();
        this.workerConnections = new HashMap<>();

        this.maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        this.sortedEntries = new ArrayList<>() {
            public boolean add(Entry mt) {
                int index = Collections.binarySearch(this, mt);
                if (index < 0) index = ~index;
                super.add(index, mt);
                return true;
            }
        };

        this.entryMap = new HashMap<>();

        this.running = true;
        this.distributeJobsThread = new Thread(() -> this.distributeJobs());
        this.distributeJobsThread.start();
    }

    public void enqueueJob(Job job) {
        try {
            this.lqueue.lock();

            while (this.jobs.size() == this.queueCap) {
                this.notFull.await();
            }

            this.jobs.add(job);
            this.notEmpty.signal();
        } catch (InterruptedException e) {
            return;
        } finally {
            this.lqueue.unlock();
        }
    }

    public void distributeJobs() {
        while (!Thread.currentThread().isInterrupted() && this.running) {
            Job job = null;
            try {
                this.lqueue.lock();

                while (this.jobs.isEmpty()) {
                    this.nWaiting += 1;
                    this.notEmpty.await();
                    this.nWaiting -= 1;
                }

                job = this.jobs.poll();
                this.notFull.signal();
            } catch (InterruptedException e) { 
                continue;
            } finally {
                this.lqueue.unlock();
            }

            try {
                this.ljobs.lock();

                if (job.getRequiredMemory() > this.memoryLimit) {
                    Packet packet = new ServerJobResultPacket(job.getId(), "Job requires more memory than the server can provide.");
                    this.sendJobResult(job.getClientName(), packet, 0, -1);
                    continue;
                }

                Entry entry = null;
                long requiredMemory = job.getRequiredMemory();
                while (entry == null) {
                    for (Entry e : this.sortedEntries) {
                        System.out.println(e);
                        if (e.availableMemory >= requiredMemory && e.availableThreads > 0) {
                            entry = e;
                            break;
                        }
                    }   
                    if (entry == null) {
                        this.hasMem.await();
                    }
                }

                try {
                    this.lwc.lock();
                    entry.availableMemory -= job.getRequiredMemory();
                    entry.availableThreads -= 1;
                    this.memoryUsed += job.getRequiredMemory();

                    WorkerConnection connection = this.workerConnections.get(entry.threadId);
                    connection.enqueueJob(job);
                } finally {
                    this.lwc.unlock();
                }
            } catch (InterruptedException e) {
                // ...
            } finally {
                this.ljobs.unlock();
            }
        }   
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

    public void addToLimits(long maxMemory, long threadId, long availableThreads) {
        try {
            this.ljobs.lock();

            this.maxHeap.add(maxMemory);
            this.memoryLimit = this.maxHeap.peek();
            this.totalMemory += maxMemory;

            Entry e = new Entry(maxMemory, threadId, availableThreads);
            this.sortedEntries.add(e);
            this.entryMap.put(threadId, e);
        } finally {
            this.ljobs.unlock();
        }
    }

    public void removeFromLimits(long maxMemory, long usedMemory, long threaId) {
        try {
            this.ljobs.lock();

            Entry e = this.entryMap.remove(threaId);
            this.sortedEntries.remove(e);

            this.totalMemory -= maxMemory;

            this.maxHeap.remove(maxMemory);
            if (this.maxHeap.isEmpty()) {
                this.memoryLimit = 0;
            } else {
                this.memoryLimit = this.maxHeap.peek();
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

    public void sendJobResult(String clientName, Packet packet, long requiredMemory, long workerThreadId) {

        try {
            this.ljobs.lock();

            Entry e = this.entryMap.get(workerThreadId);

            if (e != null) {
                e.availableMemory += requiredMemory;
                e.availableThreads += 1;
                this.memoryUsed -= requiredMemory;
            }

            try {
                this.lc.readLock().lock();
                Long threadId = this.clientThreads.get(clientName);
                if (threadId != null) {                

                    this.hasMem.signalAll();

                    ClientConnection connection = this.connections.get(threadId);
                    if (connection != null) {
                        connection.addPacketToQueue(packet);
                    } else {
                        System.out.println("Client disconnected before receiving job result.");
                    }
                } else {
                    System.out.println("Client disconnected before receiving job result.");
                }
            } finally {
                this.lc.readLock().unlock();
            }
        } finally {
            this.ljobs.unlock();
        }
    }

    public long getJobMemoryLimit() {
        return this.memoryLimit;
    }

    public long getTotalMemory() {
        return this.totalMemory;
    }

    public long getMemoryUsed() {
        return this.memoryUsed;
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

    public int getNWaiting() {
        return this.nWaiting;
    }
}