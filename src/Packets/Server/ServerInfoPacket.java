package Packets.Server;

import Packets.Packet;

public class ServerInfoPacket extends Packet {
    private final long jobMemoryLimit;
    private final long totalMemory;
    private final long memoryUsed;
    private final int queueSize;
    private final int nConnections;
    private final int nWorkers;
    private final int nWorkersWaiting;

    public ServerInfoPacket(long id, long jobMemoryLimit, long totalMemory, long memoryUsed, int queueSize, int nConnections, int nWorkers, int nWaiting) {
        super(id, ServerPacketType.INFO);
        this.jobMemoryLimit = jobMemoryLimit;
        this.totalMemory = totalMemory;
        this.memoryUsed = memoryUsed;
        this.queueSize = queueSize;
        this.nConnections = nConnections;
        this.nWorkers = nWorkers;
        this.nWorkersWaiting = nWaiting;
    }

    public long getJobMemoryLimit() {
        return this.jobMemoryLimit;
    }

    public long getTotalMemory() {
        return this.totalMemory;
    }

    public long getMemoryUsed() {
        return this.memoryUsed;
    }

    public int getQueueSize() {
        return this.queueSize;
    }

    public int getNConnections() {
        return this.nConnections;
    }

    public int getNWorkers() {
        return this.nWorkers;
    }

    public int getNWaiting() {
        return this.nWorkersWaiting;
    }

    @Override
    public String toString() {
        return "ServerInfoPacket{" +
                "jobMemoryLimit=" + jobMemoryLimit +
                ", totalMemory=" + totalMemory +
                ", memoryUsed=" + memoryUsed +
                ", queueSize=" + queueSize +
                ", nConnections=" + nConnections +
                ", nWorkers=" + nWorkers +
                ", nWorkersWaiting=" + nWorkersWaiting +
                '}';
    }

}
