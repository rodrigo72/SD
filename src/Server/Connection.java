package Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import Packets.Deserializer;
import Packets.Serializer;
import Packets.Packet;
import Utils.ConditionQueue;

public abstract class Connection implements Runnable {

    private DataOutputStream out;
    private DataInputStream in;
    private Socket socket;
    private Serializer serializer;
    private Deserializer deserializer;
    protected boolean debug;

    protected final SharedState sharedState;
    protected ConditionQueue<Packet> packetsToSend;
    protected ReentrantLock l;
    protected Thread outputThread;
    protected long threadId;

    public Connection(SharedState sharedState, Socket socket, boolean debug) {
        
        this.sharedState = sharedState;
        this.socket = socket;
        this.debug = debug;
        this.l = new ReentrantLock();
        this.packetsToSend = new ConditionQueue<>(this.l);
        this.threadId = -1;
        this.serializer = null;
        this.deserializer = null;

        try {
            this.out = new DataOutputStream(this.socket.getOutputStream());
            this.in = new DataInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            if (this.debug)
                System.out.println("Connection: " + e.getMessage());
        }

        this.outputThread = new Thread(() -> this.sendPackets());
    }

    public void startOutputThread() {
        this.outputThread.start();
    }

    public void sendPackets() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.l.lock();

                while (this.packetsToSend.queue.isEmpty()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    } else {
                        this.packetsToSend.notEmpty.await();
                    }
                }

                System.out.println("Sending packet.");

                Packet packet = this.packetsToSend.queue.poll();
                this.serializer.serialize(out, packet);

            } catch (IOException e) {
                if (this.debug)
                    System.out.println("sendPackets: " +  e);
            } catch (InterruptedException e) {
                // nothing
            } finally {
                this.l.unlock();
            }
        }
    }

    public void addPacketToQueue(Packet packet) {
        try {
            this.l.lock();
            System.out.println("Added packet to queue.");
            this.packetsToSend.queue.add(packet);
            this.packetsToSend.notEmpty.signal();
        } finally {
            this.l.unlock();
        }
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    public void setDeserializer(Deserializer deserializer) {
        this.deserializer = deserializer;
    }

    public void serialize(Packet p) throws IOException {
        this.serializer.serialize(this.out, p);
    }

    public Packet deserialize() throws IOException {
        return this.deserializer.deserialize(this.in);
    }
}