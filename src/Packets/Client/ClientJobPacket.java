package Packets.Client;

import Packets.Packet;

public class ClientJobPacket extends Packet {
    private final long requiredMemory;
    private final byte[] data;

    public ClientJobPacket(long id, long requiredMemory, byte[] data) {
        super(id, ClientPacketType.JOB);
        this.requiredMemory = requiredMemory;
        this.data = data;
    }

    public long getRequiredMemory() {
        return this.requiredMemory;
    }

    public byte[] getData() {
        return this.data;
    }

    public String toString() {
        return super.toString() + ", " + this.requiredMemory + ", " + this.data.length + " }";
    }
}
