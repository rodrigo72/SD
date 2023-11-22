package Packets.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import Packets.Packet;
import Packets.PacketType;
import Packets.Serializer;

public class ServerPacketSerializer implements Serializer {
    public void serialize(DataOutputStream out, Packet packet) throws IOException {
        PacketType.serialize(packet.getType(), out);
        out.writeLong(packet.getId());

        switch ((ServerPacketType) packet.getType()) {
            case STATUS -> {
                ServerStatusPacket statusPacket = (ServerStatusPacket) packet;
                serializeStatusPacket(out, statusPacket);
            }
            case JOB_RESULT -> {
                ServerJobResultPacket jobResultPacket = (ServerJobResultPacket) packet;
                serializeJobResultPacket(out, jobResultPacket);
            }
            default -> { return; }
        }
    }

    private static void serializeJobResultPacket(DataOutputStream out, ServerJobResultPacket packet) throws IOException {
        ServerJobResultPacket.ResultStatus status = packet.getStatus();
        out.writeInt(status.ordinal());
        if (status == ServerJobResultPacket.ResultStatus.FAILURE) {
            out.writeUTF(packet.getErrorMessage());
        } else {
            out.writeInt(packet.getData().length);
            out.write(packet.getData());
        }
    }

    private static void serializeStatusPacket(DataOutputStream out, ServerStatusPacket packet) throws IOException {
        out.writeInt(packet.getStatus().ordinal());
    }
}
