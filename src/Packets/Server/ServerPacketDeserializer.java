package Packets.Server;

import Packets.Packet;
import Packets.PacketType;
import java.io.DataInputStream;
import java.io.IOException;
import Packets.Deserializer;

public class ServerPacketDeserializer implements Deserializer {
    public Packet deserialize(DataInputStream in) throws IOException {
        ServerPacketType type = PacketType.deserialize(in, ServerPacketType.class);
        long id = in.readLong();

        switch (type) {
            case STATUS:
                return deserializeStatusPacket(id, in);
            default:
                return null;
        }
    }

    private static Packet deserializeStatusPacket(long id, DataInputStream in) throws IOException {
        ServerStatusPacket.Status status = ServerStatusPacket.Status.values()[in.readInt()];
        return new ServerStatusPacket(id, status);
    }
}
