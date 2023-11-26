package Server;

import java.io.*;
import java.net.Socket;
import Packets.Packet;
import Packets.Server.*;
import Packets.Client.*;
import Exceptions.*;

public class ClientConnection extends Connection {

    private RegistrationManager registrationManager;
    private boolean loggedIn;
    private String clientName;

    public ClientConnection(SharedState sharedState, Socket socket, RegistrationManager rm, boolean debug) {
        
        super(sharedState, socket, debug);
        this.registrationManager = rm;
        this.clientName = null;
        this.loggedIn = false;
        this.setDeserializer(new ClientPacketDeserializer());
        this.setSerializer(new ServerPacketSerializer());
        this.startOutputThread();
    }

    @Override
    public void run() {
        try {
            this.threadId = Thread.currentThread().getId();
            while (true) {
                
                Packet p = this.deserialize();
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
            this.addPacketToQueue(statusPacket);
            return;
        }

        ClientJobPacket packet = (ClientJobPacket) p;
        Job job = new Job(this.clientName, packet.getRequiredMemory(), id, packet.getData());

        this.sharedState.enqueueJob(job);
    }
    
    private void handleInfoPacket(Packet p, long id) throws IOException {
        if (!this.loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.addPacketToQueue(statusPacket);
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

        this.addPacketToQueue(packet);
    }

}