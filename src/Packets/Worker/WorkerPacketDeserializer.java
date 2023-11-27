package Packets.Worker;

import java.io.DataInputStream;
import java.io.IOException;

import Packets.Deserializer;
import Packets.Packet;
import Packets.PacketType;

public class WorkerPacketDeserializer implements Deserializer {

    @Override
    public Packet deserialize(DataInputStream in) throws IOException {
        WorkerPacketType type = PacketType.deserialize(in, WorkerPacketType.class);
        long id = in.readLong();

        switch (type) {
            case CONNECTION -> {
                return deserializeConnectionPacket(id, in);
            }
            case JOB_RESULT -> {
                return deserializeJobResultPacket(id, in);
            }
            case DISCONNECTION -> {
                return deserializeDisconnectionPacket(id, in);
            } 
            default -> {
                return null;
            }
        }

    }

    private static Packet deserializeDisconnectionPacket(long id, DataInputStream in) {
        return new WorkerDisconnectionPacket(id);
    }

    private static Packet deserializeConnectionPacket(long id, DataInputStream in) throws IOException {
        long maxMemory = in.readLong();
        int nThreads = in.readInt();
        return new WorkerConnectionPacket(id, maxMemory, nThreads);
    }

    private static Packet deserializeJobResultPacket(long id, DataInputStream in) throws IOException {
        WorkerJobResultPacket.ResultStatus status = WorkerJobResultPacket.ResultStatus.values()[in.readInt()];
        String clientName = in.readUTF();
        if (status == WorkerJobResultPacket.ResultStatus.FAILURE) {
            String errorMessage = in.readUTF();
            return new WorkerJobResultPacket(id, errorMessage, clientName);
        } else {
            int dataLength = in.readInt();
            byte[] data = new byte[dataLength];
            in.readFully(data);
            return new WorkerJobResultPacket(id, data, clientName);
        }
    }
    
}
