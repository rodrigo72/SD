package Packets.Server;

import Packets.Packet;

public class ServerInfoPacket extends Packet {
    private final long maxMemory;
    private final long availableMemory;
    private final int queueSize;
    private final int nConnections;
    private final int nWorkers;
    private final int nWorkersWaiting;

    public ServerInfoPacket(long id, long maxMemory, long availableMemory, 
        int queueSize, int nConnections, int nWorkers, int nWorkersWaiting) {
        super(id, ServerPacketType.INFO);
        this.maxMemory = maxMemory;
        this.availableMemory = availableMemory;
        this.queueSize = queueSize;
        this.nConnections = nConnections;
        this.nWorkers = nWorkers;
        this.nWorkersWaiting = nWorkersWaiting;
    }

    public long getMaxMemory()          { return this.maxMemory; }
    public long getAvailableMemory()    { return this.availableMemory; }
    public int getQueueSize()           { return this.queueSize; }
    public int getNConnections()        { return this.nConnections; }
    public int getNWorkers()            { return this.nWorkers; }
    public int getNWorkersWaiting()     { return this.nWorkersWaiting; }

    public String toString() {
        return super.toString() + ", Max memory: " + this.maxMemory + ", Available memory: " 
        + this.availableMemory + ", Queue size: " + this.queueSize + ", Nº connections: " 
        + this.nConnections + ", Nº workers: " + this.nWorkers + ", Nº workers waiting: "
        + this.nWorkersWaiting + " }";
    }
}
