package Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import Packets.Deserializer;
import Packets.Serializer;
import Packets.Packet;
import Packets.Server.*;
import Packets.Client.*;
import Exceptions.Registration.*;
import Utils.ConditionQueue;

public class Connection implements Runnable {

    private final SharedState sharedState;
    private DataOutputStream out;
    private DataInputStream in;
    private Socket clientSocket;
    private Serializer serializer;
    private Deserializer deserializer;
    private RegistrationManager registrationManager;
    private boolean loggedIn;
    private boolean debug;
    private String clientName;
    private ConditionQueue<Packet> packetsToSend;
    private ReentrantLock l;
    private Thread outputThread;
    private long threadId;

    public Connection(SharedState sharedState, Socket socket, RegistrationManager rm, boolean debug) {
        
        this.sharedState = sharedState;
        this.clientSocket = socket;
        this.registrationManager = rm;
        this.debug = debug;
        this.loggedIn = false;
        this.clientName = null;
        this.serializer = new ServerPacketSerializer();
        this.deserializer = new ClientPacketDeserializer();
        this.l = new ReentrantLock();
        this.packetsToSend = new ConditionQueue<>(this.l);
        this.threadId = -1;

        try {
            this.out = new DataOutputStream(this.clientSocket.getOutputStream());
            this.in = new DataInputStream(this.clientSocket.getInputStream());
        } catch (IOException e) {
            if (this.debug)
                System.out.println("Connection: " + e.getMessage());
        }

        this.outputThread = new Thread(() -> this.sendPackets());
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

    @Override
    public void run() {
        try {
            this.threadId = Thread.currentThread().getId();
            while (true) {
                
                Packet p = this.deserializer.deserialize(this.in);
                long id = p.getId();
                ClientPacketType type = (ClientPacketType) p.getType();

                if (this.debug)
                    System.out.println("Received packet of type " + type);

                switch (type) {
                    case REGISTRATION -> {
                        this.handleRegistrationPacket(p, id);
                    }
                    case LOGIN -> {
                        this.handleLoginPacket(p, id);
                    }
                    case LOGOUT -> {
                        this.handleLogoutPacket(p, id);
                    }
                    case JOB -> {
                        this.handleJobPacket(p, id);
                    }
                    case GET_INFO -> {
                        this.handleInfoPacket(p, id);
                    }
                    default -> {
                        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
                        this.addPacketToQueue(statusPacket);
                        if (this.debug)
                            System.out.println("Sent packet: " + statusPacket);
                    }
                }
            }
        } catch (IOException e) {
            if (this.debug)
                System.out.println("Connection: " + e.getMessage());

            try {
                this.l.lock();
                this.outputThread.interrupt();
                this.packetsToSend.notEmpty.signal();
            } finally {
                this.l.unlock();
            }

            this.sharedState.removeConnection(threadId);
            this.sharedState.removeClientThread(clientName);

            try {
                this.registrationManager.logout(clientName);
            } catch (RegistrationDoesNotExist e2) {
                // nothing
            }
        }
    }

    private void handleRegistrationPacket(Packet p, long id) throws IOException {
        if (this.loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.addPacketToQueue(statusPacket);
            return;
        }

        ClientRegistrationPacket packet = (ClientRegistrationPacket) p;
        String name = packet.getName();
        String password = packet.getPassword();

        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.SUCCESS);

        try {
            this.registrationManager.register(name, password);
            this.loggedIn = true;
            this.clientName = name;
            this.sharedState.addClientThread(name, this.threadId);
        } catch (AlreadyRegisteredException e) {
            statusPacket.setStatus(ServerStatusPacket.Status.ERROR);
            this.loggedIn = false;
        }

        this.addPacketToQueue(statusPacket);
    }

    private void handleLoginPacket(Packet p, long id) throws IOException {
        if (this.loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.addPacketToQueue(statusPacket);
            return;
        }

        ClientLoginPacket packet = (ClientLoginPacket) p;
        String name = packet.getName();
        String password = packet.getPassword();

        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.SUCCESS);

        try {
            this.registrationManager.login(name, password);
            this.loggedIn = true;
            this.clientName = name;
            this.sharedState.addClientThread(name, this.threadId);
        } catch (RegistrationDoesNotExist | InvalidPasswordException | AlreadyConnectedException e) {
            statusPacket.setStatus(ServerStatusPacket.Status.ERROR);
            this.loggedIn = false;
        }

        this.addPacketToQueue(statusPacket);
    }

    private void handleLogoutPacket(Packet p, long id) throws IOException {
        if (!this.loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.addPacketToQueue(statusPacket);
            return;
        }

        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.SUCCESS);

        try {
            this.registrationManager.logout(clientName);
            this.loggedIn = false;
            this.sharedState.removeClientThread(clientName);
        } catch (RegistrationDoesNotExist e) {
            statusPacket.setStatus(ServerStatusPacket.Status.ERROR);
        }

        this.addPacketToQueue(statusPacket);
    }

    private void handleJobPacket(Packet p, long id) throws IOException {
        if (!this.loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.serializer.serialize(out, statusPacket);
            return;
        }

        ClientJobPacket packet = (ClientJobPacket) p;
        Job job = new Job(this.clientName, packet.getRequiredMemory(), id, packet.getData());

        this.sharedState.enqueueJob(job);
    }
    
    private void handleInfoPacket(Packet p, long id) throws IOException {
        if (!this.loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.serializer.serialize(out, statusPacket);
            return;
        }

        ServerInfoPacket packet = new ServerInfoPacket(
            id,
            this.sharedState.getMaxMemory(),
            this.sharedState.getAvailableMemory(),
            this.sharedState.getQueueSize(),
            this.sharedState.getNConnections(),
            this.sharedState.getNWorkers(),
            this.sharedState.getNWorkersWaiting()
        );

        this.serializer.serialize(out, packet);
    }

}