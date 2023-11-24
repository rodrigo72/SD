package Client;

import java.io.IOException;
import Packets.Packet;
import java.util.List;

public interface ClientAPI {
    void createRegistration(String name, String password);
    long sendRegistration() throws Exception;
    long sendLogin() throws Exception;
    long sendLogout() throws Exception;
    long sendJob(int requiredMemory, byte[] job) throws Exception;
    Packet receive(long id) throws IOException, InterruptedException;
    Packet fastReceive(long id) throws IOException, InterruptedException;
    List<Packet> getJobRequests();
    List<Packet> getJobResults();
    void exit() throws IOException;
}
