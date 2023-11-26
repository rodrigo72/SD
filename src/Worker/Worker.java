package Worker;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import Packets.Worker.*;
import Packets.Packet;
import Packets.Server.ServerJobPacket;
import Packets.Server.ServerPacketDeserializer;
import Packets.Server.ServerPacketType;
import sd23.*;

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

    public Worker(String address, int port, long maxMemory) throws IOException {
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
    }

    public void receive() {
        if (this.thread == null) {

            try {
                this.sendConnectionPacket();
            } catch (IOException e) {
                return;
            }

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

                            WorkerJobResultPacket resultPacket = null;
                            try {
                                byte[] output = JobFunction.execute(packet.getData());
                                resultPacket = new WorkerJobResultPacket(packet.getId(), output, packet.getClientName());
                            } catch (JobFunctionException e) {
                                resultPacket = new WorkerJobResultPacket(packet.getId(), e.getMessage(), packet.getClientName());
                            }
                            
                            System.out.println(resultPacket.toString());

                            try {
                                this.serializer.serialize(out, resultPacket);
                                System.out.println("Result packet sent");
                            } catch (IOException e) {
                                this.running = false;
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
    
}
