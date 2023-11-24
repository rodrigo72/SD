package Server;

import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.net.InetAddress;

public class Server {

    private int port;
    private boolean debug;
    private Map<InetAddress, Connection> connections;
    private RegistrationManager registrationManager;
    private final SharedState sharedState;

    public Server(int port, boolean debug) {
        this.port = port;
        this.debug = debug;
        this.connections = new HashMap<>();
        this.registrationManager = new RegistrationManager();
        this.sharedState = new SharedState(1000);
    }

    public void run() {
        try (ServerSocket listenSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                Connection connection = new Connection(this.sharedState, clientSocket, this.registrationManager, this.debug);
                Thread connectionThread = new Thread(connection);
                connectionThread.start();
                this.sharedState.addConnection(connection, connectionThread.getId());
                this.connections.put(clientSocket.getInetAddress(), connection);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
