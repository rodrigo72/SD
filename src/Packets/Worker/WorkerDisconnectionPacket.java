package Packets.Worker;

import Packets.Packet;

public class WorkerDisconnectionPacket extends Packet {

    public WorkerDisconnectionPacket(long id) {
        super(id, WorkerPacketType.DISCONNECTION);
    }
    
}
