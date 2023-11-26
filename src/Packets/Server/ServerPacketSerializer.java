package Packets.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import Packets.Packet;
import Packets.PacketType;
import Packets.Serializer;

public class ServerPacketSerializer implements Serializer {
    
    @Override
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
            case INFO -> {
                ServerInfoPacket infoPacket = (ServerInfoPacket) packet;
                serializeInfoPacket(out, infoPacket);
            }
            case JOB -> {
                ServerJobPacket jobPacket = (ServerJobPacket) packet;
                serializeJobPacket(out, jobPacket);
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

    private static void serializeInfoPacket(DataOutputStream out, ServerInfoPacket packet) throws IOException {
        out.writeLong(packet.getJobMemoryLimit());
        out.writeLong(packet.getTotalMemory());
        out.writeLong(packet.getMemoryUsed());
        out.writeInt(packet.getQueueSize());
        out.writeInt(packet.getNConnections());
        out.writeInt(packet.getNWorkers());
        out.writeInt(packet.getNWaiting());
    }

    private static void serializeJobPacket(DataOutputStream out, ServerJobPacket packet) throws IOException {
        out.writeUTF(packet.getClientName());
        out.writeLong(packet.getRequiredMemory());
        out.writeInt(packet.getData().length);
        out.write(packet.getData());
    }
}
