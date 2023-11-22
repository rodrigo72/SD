package Packets;

public class Packet {
    private final long id;
    private final PacketType type;

    public Packet(long id, PacketType type) {
        this.id = id;
        this.type = type;
    }

    public String toString() {
        return "Packet { " + this.id + ", " + this.type;
    }

    public long getId() {
        return this.id;
    }

    public PacketType getType() {
        return this.type;
    }
}
