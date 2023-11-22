package Server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Server {
    public static void main(String[] args) {
        int port = 8888;
        boolean debug = true;
        List<Connection> connections = new ArrayList<Connection>();
        RegistrationManager registrationManager = new RegistrationManager();

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number");
            }
        }

        try (ServerSocket listenSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                Connection c = new Connection(clientSocket, registrationManager, debug);
                connections.add(c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
    }
}
