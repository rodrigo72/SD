package Packets.Client;

import Packets.Packet;
import Packets.PacketType;
import java.io.DataInputStream;
import java.io.IOException;
import Packets.Deserializer;


public class ClientPacketDeserializer implements Deserializer {

    public Packet deserialize(DataInputStream in) throws IOException {
        ClientPacketType type = PacketType.deserialize(in, ClientPacketType.class);
        long id = in.readLong();

        switch (type) {
            case REGISTRATION:
                return deserializeRegistrationPacket(id, in);
            default:
                return null;
        }
    }

    private static Packet deserializeRegistrationPacket(long id, DataInputStream in) throws IOException {
        String name = in.readUTF();
        String password = in.readUTF();
        return new ClientRegistrationPacket(id, name, password);
    }
}

