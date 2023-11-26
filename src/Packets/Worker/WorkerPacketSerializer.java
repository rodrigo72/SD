package Packets.Worker;

import java.io.DataOutputStream;
import java.io.IOException;

import Packets.Packet;
import Packets.Serializer;
import Packets.PacketType;

public class WorkerPacketSerializer implements Serializer {

    @Override
    public void serialize(DataOutputStream out, Packet packet) throws IOException {
        PacketType.serialize(packet.getType(), out);
        out.writeLong(packet.getId());

        switch ((WorkerPacketType) packet.getType()) {
            case CONNECTION -> {
                WorkerConnectionPacket connectionPacket = (WorkerConnectionPacket) packet;
                serializeConnectionPacket(out, connectionPacket);
            }
            case JOB_RESULT -> {
                WorkerJobResultPacket jobResultPacket = (WorkerJobResultPacket) packet;
                serializeJobResultPacket(out, jobResultPacket);
            }
            case DISCONNECTION -> {
                // No additional data to serialize
            }
        }
    }
    
    private static void serializeConnectionPacket(DataOutputStream out, WorkerConnectionPacket packet) throws IOException {
        out.writeLong(packet.getMaxMemory());
    }

    private static void serializeJobResultPacket(DataOutputStream out, WorkerJobResultPacket packet) throws IOException {
        WorkerJobResultPacket.ResultStatus status = packet.getStatus();
        out.writeInt(status.ordinal());
        out.writeUTF(packet.getClientName());
        if (status == WorkerJobResultPacket.ResultStatus.FAILURE) {
            out.writeUTF(packet.getErrorMessage());
        } else {
            out.writeInt(packet.getData().length);
            out.write(packet.getData());
        }
    }

}
