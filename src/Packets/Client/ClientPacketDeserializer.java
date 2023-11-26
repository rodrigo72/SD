package Packets.Client;

import Packets.Packet;
import Packets.PacketType;
import java.io.DataInputStream;
import java.io.IOException;
import Packets.Deserializer;


public class ClientPacketDeserializer implements Deserializer {

    @Override
    public Packet deserialize(DataInputStream in) throws IOException {
        ClientPacketType type = PacketType.deserialize(in, ClientPacketType.class);
        long id = in.readLong();

        switch (type) {
            case REGISTRATION -> { return deserializeRegistrationPacket(id, in); }
            case LOGIN -> { return deserializeLoginPacket(id, in); }
            case LOGOUT -> { return deserializeLogoutPacket(id, in); }
            case JOB -> { return deserializeJobPacket(id, in); }
            case GET_INFO -> { return deserializeServerStatusPacket(id, in); }
            default -> { return null; }
        }
    }

    private static Packet deserializeJobPacket(long id, DataInputStream in) throws IOException {
        long requiredMemory = in.readLong();
        int dataLength = in.readInt();
        byte[] data = new byte[dataLength];
        in.readFully(data);
        return new ClientJobPacket(id, requiredMemory, data);
    }

    private static Packet deserializeRegistrationPacket(long id, DataInputStream in) throws IOException {
        String name = in.readUTF();
        String password = in.readUTF();
        return new ClientRegistrationPacket(id, name, password);
    }

    private static Packet deserializeLoginPacket(long id, DataInputStream in) throws IOException {
        String name = in.readUTF();
        String password = in.readUTF();
        return new ClientLoginPacket(id, name, password);
    }

    private static Packet deserializeLogoutPacket(long id, DataInputStream in) throws IOException {
        return new ClientLogoutPacket(id);
    }

    private static Packet deserializeServerStatusPacket(long id, DataInputStream in) throws IOException {
        return new ClientServerStatusPacket(id);
    }
}

