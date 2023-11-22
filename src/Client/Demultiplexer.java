package Client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.io.DataInputStream;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.ArrayDeque;
import Packets.Packet;
import Packets.Deserializer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Demultiplexer implements Runnable {
    
    private Map<Long, Entry> map;
    private final DataInputStream in;
    private ReentrantLock l;
    private Deserializer deserializer;
    private Thread thread;
    private volatile boolean running;

    private class Entry {
        int n_waiting;
        Queue<Packet> queue;
        Condition notEmpty;

        public Entry() {
            this.n_waiting = 0;
            this.queue = new ArrayDeque<>();
            this.notEmpty = l.newCondition();
        }
    }

    public Demultiplexer(DataInputStream in, Deserializer deserializer) {
        this.in = in;
        this.running = false;
        this.thread = null;
        this.map = new HashMap<>();
        this.l = new ReentrantLock();
        this.deserializer = deserializer;
    }

    public void run() {
        this.thread = new Thread(() -> {
            while (this.running) {
                Packet packet = null;
                try {
                    packet = this.deserializer.deserialize(this.in);
                } catch (IOException e) {
                    this.running = false;
                    System.out.println("Demultiplexer: " + e.getMessage());
                    break;
                }

                try {
                    this.l.lockInterruptibly();

                    Entry entry = this.map.get(packet.getId());
                    if (entry == null) {
                        entry = new Entry();
                        this.map.put(packet.getId(), entry);
                    }

                    entry.queue.add(packet);
                    entry.notEmpty.signal();
                } catch (InterruptedException e) {
                    this.running = false;
                    System.out.println("Demultiplexer was interrupted.");
                } finally {
                    this.l.unlock();
                }
            }
        });
        this.running = true;
        this.thread.start();
    }

    public Packet fastReceive(long id) throws IOException {
        try {
            this.l.lock();
            Entry entry = this.map.get(id);
            if (entry == null)
                return null;

            if (entry.queue.isEmpty())
                return null;
            
            Packet reply = entry.queue.poll();
            if (entry.n_waiting == 0 && entry.queue.isEmpty()) {
                this.map.remove(id);
            }

            return reply;
        } finally {
            this.l.unlock();
        }
    }

    public Packet receive(long id) throws InterruptedException {
        try {
            this.l.lock();
            
            Entry entry = this.map.get(id);
            if (entry == null) {
                entry = new Entry();
                this.map.put(id, entry);
            }

            entry.n_waiting++;

            while (true) {
                if (!entry.queue.isEmpty()) {
                    entry.n_waiting--;
                    Packet reply = entry.queue.poll();
                    if (entry.n_waiting == 0 && entry.queue.isEmpty()) {
                        this.map.remove(id);
                    }
                    return reply;
                } else {
                    entry.notEmpty.await(5, TimeUnit.MINUTES);
                }
            }
        } finally {
            this.l.unlock();
        }
    }

    public void stop() throws IOException {
        this.running = false;
        this.in.close();
        if (this.thread != null)
            this.thread.interrupt();
            this.thread = null;
    }
    
}
