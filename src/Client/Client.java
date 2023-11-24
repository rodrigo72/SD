package Client;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import Packets.Client.*;
import Packets.Server.ServerPacketDeserializer;
import Packets.Server.ServerPacketType;
import Packets.Server.ServerJobResultPacket;
import Packets.Packet;
import Utils.Demultiplexer;
import java.util.List;

public class Client implements ClientAPI {

    private long nextId;
    private RegistrationMoreInfo registration;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final Socket socket;
    private final ClientPacketSerializer serializer;
    private final Demultiplexer demultiplexer;
    private final ServerPacketDeserializer deserializer;
    private final JobManager jobManager;
    
    public Client(String address, int port, JobManager jobManager) throws IOException {
        this.socket = new Socket(address, port);
        this.jobManager = jobManager;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.registration = null;
        this.nextId = 0L;
        this.serializer = new ClientPacketSerializer();
        this.deserializer = new ServerPacketDeserializer();
        this.demultiplexer = new Demultiplexer(this.in, this.deserializer);
        this.demultiplexer.run();
        this.hook(socket);
    }

    private void hook(Socket socket) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Client: " + e.getMessage());
                }
            }
        });
    }

    private long getNextId() {
        return this.nextId++;
    }

    public void createRegistration(String name, String password) {
        this.registration = new RegistrationMoreInfo(name, password, true);
    }

    public long sendRegistration() throws IOException {
        long id = this.getNextId();
        ClientRegistrationPacket packet = new ClientRegistrationPacket(
            id, 
            this.registration.getName(), 
            this.registration.getPassword()
        );
        this.serializer.serialize(this.out, packet);
        return id;
    }

    public long sendLogin() throws IOException {
        long id = this.getNextId();
        ClientLoginPacket packet = new ClientLoginPacket(
            id, 
            this.registration.getName(), 
            this.registration.getPassword()
        );
        this.serializer.serialize(out, packet);
        return id;
    }

    public long sendLogout() throws IOException {
        long id = this.getNextId();
        ClientLogoutPacket packet = new ClientLogoutPacket(id);
        this.serializer.serialize(out, packet);
        return id;
    }

    public long sendJob(int requiredMemory, byte[] job) throws IOException {
        long id = this.getNextId();
        ClientJobPacket packet = new ClientJobPacket(id, requiredMemory, job);
        this.serializer.serialize(out, packet);
        this.registration.addJobRequest(packet);
        return id;
    }

    public Packet receive(long id) throws IOException, InterruptedException {
        Packet packet = this.demultiplexer.receive(id);
        if (packet.getType() == ServerPacketType.JOB_RESULT) {
            this.registration.addJobResult(packet);
            this.saveResult(packet);
        }
        return packet;
    }

    private void saveResult(Packet packet) throws IOException {
        if (packet.getType() != ServerPacketType.JOB_RESULT)
            return;
        ServerJobResultPacket p = (ServerJobResultPacket) packet;
        if (p.getStatus() == ServerJobResultPacket.ResultStatus.SUCCESS)
            this.jobManager.saveJobResult("Result", packet.getId(), p.getData());
    }

    public Packet fastReceive(long id) throws IOException, InterruptedException {
        Packet packet = this.demultiplexer.fastReceive(id);
        if (packet != null && packet.getType() == ServerPacketType.JOB_RESULT) {
            this.registration.addJobResult(packet);
            this.saveResult(packet);
        }
        return packet;
    }

    public List<Packet> getJobRequests() {
        return this.registration.getJobRequests();
    }

    public List<Packet> getJobResults() {
        // snapshot
        for (long id : this.registration.getWaiting()) {
            try {
                this.fastReceive(id);
            } catch (IOException | InterruptedException e) {
                System.out.println("Client: " + e.getMessage());
            }
        }

        return this.registration.getJobResults();
    }

    public void exit() throws IOException {
        this.demultiplexer.stop();
        this.socket.close();
    }
}
