package Packets.Worker;

import Packets.Packet;

public class WorkerConnectionPacket extends Packet {

    private final long maxMemory;

    public WorkerConnectionPacket(long id, long maxMemory) {
        super(id, WorkerPacketType.CONNECTION);
        this.maxMemory = maxMemory;
    }

    public long getMaxMemory() {
        return this.maxMemory;
    }
}
