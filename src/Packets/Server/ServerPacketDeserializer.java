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
            case STATUS -> { return deserializeStatusPacket(id, in); }
            case JOB_RESULT -> { return deserializeJobResultPacket(id, in); }
            default -> { return null; }
        }
    }

    private static Packet deserializeJobResultPacket(long id, DataInputStream in) throws IOException {
        ServerJobResultPacket.ResultStatus status = ServerJobResultPacket.ResultStatus.values()[in.readInt()];
        if (status == ServerJobResultPacket.ResultStatus.FAILURE) {
            String errorMessage = in.readUTF();
            return new ServerJobResultPacket(id, errorMessage);
        } else {
            int dataLength = in.readInt();
            byte[] data = new byte[dataLength];
            in.readFully(data);
            return new ServerJobResultPacket(id, data);
        }
    }

    private static Packet deserializeStatusPacket(long id, DataInputStream in) throws IOException {
        ServerStatusPacket.Status status = ServerStatusPacket.Status.values()[in.readInt()];
        return new ServerStatusPacket(id, status);
    }
}
