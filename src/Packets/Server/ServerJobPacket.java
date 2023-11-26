package Packets.Server;

import Packets.Packet;

public class ServerJobPacket extends Packet {
    private final String clientName;
    private final long requiredMemory;
    private final byte[] data;

    public ServerJobPacket(long id, String clientName, long requiredMemory, byte[] data) {
        super(id, ServerPacketType.JOB);
        this.clientName = clientName;
        this.requiredMemory = requiredMemory;
        this.data = data;
    }

    public String getClientName() {
        return this.clientName;
    }

    public long getRequiredMemory() {
        return this.requiredMemory;
    }

    public byte[] getData() {
        return this.data;
    }

    public String toString() {
        return super.toString() + ", " + this.clientName + ", " + this.requiredMemory + ", " + this.data.length + " }";
    }
}
