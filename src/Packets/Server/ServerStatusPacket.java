package Packets.Server;

import Packets.Packet;

public class ServerStatusPacket extends Packet{
    public enum Status {
        SUCCESS,
        ERROR,
        INVALID;
    }

    private Status status;

    public ServerStatusPacket(long id, Status status) {
        super(id, ServerPacketType.STATUS);
        this.status = status;
    }

    public Status getStatus() {
        return this.status;
    }

    public String toString() {
        return super.toString() + ", " + this.status + " }";
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    
}
