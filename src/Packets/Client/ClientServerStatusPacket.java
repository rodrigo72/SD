package Packets.Client;

import Packets.Packet;

public class ClientServerStatusPacket extends Packet {
    
    public ClientServerStatusPacket(long id) {
        super(id, ClientPacketType.GET_INFO);
    }

    public String toString() {
        return super.toString() + " }";
    }
    
}
