package Server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import Packets.Deserializer;
import Packets.Serializer;
import Packets.Packet;
import Packets.Server.*;
import Packets.Client.*;
import Exceptions.Registration.*;
import sd23.*;

public class Server {

    // private static final long MAX_MEMORY = 1024 * 1024 * 1024 * 32; // 32 GB in bytes (RAM)
    private int port;
    private boolean debug;
    private List<Thread> connections;
    private RegistrationManager registrationManager;
    private Deserializer deserializer;
    private Serializer serializer;

    public Server(int port, boolean debug) {
        this.port = port;
        this.debug = debug;
        this.connections = new ArrayList<>();
        this.registrationManager = new RegistrationManager();
        this.deserializer = new ClientPacketDeserializer();
        this.serializer = new ServerPacketSerializer();
    }

    public void run() {
        try (ServerSocket listenSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                Thread connection = new Thread(() -> this.connectionHandler(clientSocket));
                connection.start();
                this.connections.add(connection);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void connectionHandler(Socket clientSocket) {
        boolean loggedIn = false;
        try {
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        
            while (true) {
                if (this.debug)
                    System.out.println("Waiting for packet...");

                Packet p = this.deserializer.deserialize(in);
                long id = p.getId();
                ClientPacketType type = (ClientPacketType) p.getType();

                if (this.debug)
                    System.out.println("Received packet: " + p);

                
                switch (type) {
                    case REGISTRATION -> {
                        loggedIn = this.handleRegistrationPacket(out, p, id, loggedIn);
                    }
                    case LOGIN -> {
                        loggedIn = this.handleLoginPacket(out, p, id, loggedIn);
                    }
                    case LOGOUT -> {
                        loggedIn = this.handleLogoutPacket(out, p, id, loggedIn);
                    }
                    case JOB -> {
                        this.handleJobPacket(out, p, id, loggedIn);
                    }
                    default -> {
                        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
                        this.serializer.serialize(out, statusPacket);
                        if (this.debug)
                            System.out.println("Sent packet: " + statusPacket);
                    }
                }
            }
        } catch (IOException e) {
            if (this.debug)
                System.out.println("Connection: " + e.getMessage());
        }
    }

    private void handleJobPacket(DataOutputStream out, Packet p, long id, boolean loggedIn) throws IOException {
        if (!loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.serializer.serialize(out, statusPacket);
            return;
        }

        ClientJobPacket packet = (ClientJobPacket) p;
        // long requiredMemory = packet.getRequiredMemory();
        byte[] data = packet.getData();

        // Just testing for now, this will be done with a thread pool (probably)
        try {
            byte[] output = JobFunction.execute(data);
            ServerJobResultPacket resultPacket = new ServerJobResultPacket(id, output);
            this.serializer.serialize(out, resultPacket);
        } catch (JobFunctionException e) {
            ServerJobResultPacket resultPacket = new ServerJobResultPacket(id, e.getMessage());
            this.serializer.serialize(out, resultPacket);
        }
    }

    private boolean handleRegistrationPacket(DataOutputStream out, Packet p, long id, boolean loggedIn) throws IOException {

        boolean loggInState = loggedIn;
        if (loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.serializer.serialize(out, statusPacket);
            return loggInState;
        }

        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.SUCCESS);
        ClientRegistrationPacket packet = (ClientRegistrationPacket) p;
        String name = packet.getName();
        String password = packet.getPassword();

        try {
            this.registrationManager.register(name, password);
            loggInState = true;
        } catch (AlreadyRegisteredException e) {
            statusPacket.setStatus(ServerStatusPacket.Status.ERROR);
            loggInState = false;
        }

        this.serializer.serialize(out, statusPacket);
        return loggInState;
   }

    private boolean handleLoginPacket(DataOutputStream out, Packet p, long id, boolean loggedIn) throws IOException {

        boolean loggInState = loggedIn;
        if (loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.serializer.serialize(out, statusPacket);
            return loggInState;
        }

        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.SUCCESS);
        ClientLoginPacket packet = (ClientLoginPacket) p;
        String name = packet.getName();
        String password = packet.getPassword();

        try {
            this.registrationManager.login(name, password);
            loggInState = true;
        } catch (RegistrationDoesNotExist | InvalidPasswordException e) {
            statusPacket.setStatus(ServerStatusPacket.Status.ERROR);
            loggInState = false;
        }

        this.serializer.serialize(out, statusPacket);
        return loggInState;
    }

    private boolean handleLogoutPacket(DataOutputStream out, Packet p, long id, boolean loggedIn) throws IOException {
        boolean loggInState = loggedIn;
        if (!loggedIn) {
            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
            this.serializer.serialize(out, statusPacket);
            return loggInState;
        }

        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.SUCCESS);
        this.serializer.serialize(out, statusPacket);
        return false;
    }

    public static void main(String[] args) {
        int port = 8888;
        boolean debug = true;

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number");
            }
        }

        Server server = new Server(port, debug);
        server.run();
        
    }
}
