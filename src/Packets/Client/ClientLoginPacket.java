package Packets.Client;

import Packets.Packet;

public class ClientLoginPacket extends Packet {
    private final String name;
    private final String password;

    public ClientLoginPacket(long id, String name, String password) {
        super(id, ClientPacketType.LOGIN);
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return this.password;
    }

    public String toString() {
        return super.toString() + ", " + this.name + ", " + this.password + " }";
    }
}
