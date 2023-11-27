package Packets.Worker;

import Packets.Packet;

public class WorkerConnectionPacket extends Packet {

    private final long maxMemory;
    private final int nThreads;

    public WorkerConnectionPacket(long id, long maxMemory, int nThreads) {
        super(id, WorkerPacketType.CONNECTION);
        this.maxMemory = maxMemory;
        this.nThreads = nThreads;
    }

    public long getMaxMemory() {
        return this.maxMemory;
    }

    public int getNThreads() {
        return this.nThreads;
    }
}
