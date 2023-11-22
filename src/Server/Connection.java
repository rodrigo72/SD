package Server;

import java.io.*;
import java.net.Socket;

import Packets.Deserializer;
import Packets.Serializer;
import Packets.Packet;
import Packets.Server.*;
import Packets.Client.*;
import Exceptions.Registration.*;

public class Connection extends Thread {
    private DataOutputStream out;
    private DataInputStream in;
    private Socket clientSocket;
    private Deserializer deserializer;
    private Serializer serializer;
    private boolean loggedIn;
    private boolean debug;
    private RegistrationManager registrationManager;

    public Connection(Socket clientSocket, RegistrationManager rm, boolean debug) {
        try {
            this.loggedIn = false;
            this.debug = debug;
            this.clientSocket = clientSocket;
            this.registrationManager = rm;
            this.out = new DataOutputStream(this.clientSocket.getOutputStream());
            this.in = new DataInputStream(this.clientSocket.getInputStream());
            this.deserializer = new ClientPacketDeserializer();
            this.serializer = new ServerPacketSerializer();
            this.start();
        } catch (IOException e) {
            if (this.debug)
                System.out.println("Connection: " + e.getMessage());
        }
    }

    public void run() {
        try {
            while (true) {
                if (this.debug)
                    System.out.println("Waiting for packet...");

                Packet p = this.deserializer.deserialize(this.in);
                long id = p.getId();
                ClientPacketType type = (ClientPacketType) p.getType();

                if (this.debug)
                    System.out.println("Received packet: " + p);

                switch (type) {
                    case REGISTRATION:

                        if (this.loggedIn) {
                            ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.ERROR);
                            this.serializer.serialize(out, statusPacket);
                            break;
                        }

                        ServerStatusPacket statusPacket = new ServerStatusPacket(id, ServerStatusPacket.Status.SUCCESS);
                        ClientRegistrationPacket packet = (ClientRegistrationPacket) p;
                        String name = packet.getName();
                        String password = packet.getPassword();

                        try {
                            this.registrationManager.register(name, password);
                            this.loggedIn = true;
                        } catch (AlreadyRegisteredException e) {
                            statusPacket.setStatus(ServerStatusPacket.Status.ERROR);
                            this.loggedIn = false;
                        }

                        this.serializer.serialize(out, statusPacket);
                        if (this.debug)
                            System.out.println("Sent packet: " + statusPacket);
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            if (this.debug)
                System.out.println("Connection: " + e.getMessage());
        }
    }
}
