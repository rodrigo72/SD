package Packets.Client;

import Packets.Packet;

public class ClientLogoutPacket extends Packet {
    public ClientLogoutPacket(long id) {
        super(id, ClientPacketType.LOGOUT);
    }

    public String toString() {
        return super.toString() + " }";
    }
}
