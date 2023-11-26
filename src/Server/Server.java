package Server;

import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;

public class Server {

    private int port;
    private boolean debug;
    private Map<InetAddress, ClientConnection> connections;
    private RegistrationManager registrationManager;
    private final SharedState sharedState;

    public Server(int port, boolean debug) {
        this.port = port;
        this.debug = debug;
        this.connections = new HashMap<>();
        this.registrationManager = new RegistrationManager();
        this.sharedState = new SharedState();
    }

    public void run() {

        Set<InetAddress> workers = new HashSet<>();
        try {
            workers.add(InetAddress.getByName("10.4.4.2"));
            workers.add(InetAddress.getByName("10.3.3.1"));
            workers.add(InetAddress.getByName("10.3.3.2"));
        } catch (UnknownHostException e) {
            // do nothing
        }
    
        try (ServerSocket listenSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket clientSocket = listenSocket.accept();
                InetAddress clientAddress = clientSocket.getInetAddress();

                if (workers.contains(clientAddress)) {    
                    WorkerConnection workerConnection = new WorkerConnection(sharedState, clientSocket, this.debug);
                    Thread workerConnectionThread = new Thread(workerConnection);
                    workerConnectionThread.start();
                    this.sharedState.addWorkerConnection(workerConnection, workerConnectionThread.getId());
                } else {
                    ClientConnection connection = new ClientConnection(this.sharedState, clientSocket, this.registrationManager, this.debug);
                    Thread connectionThread = new Thread(connection);
                    connectionThread.start();
                    this.sharedState.addConnection(connection, connectionThread.getId());
                    this.connections.put(clientAddress, connection);
                }

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            
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
