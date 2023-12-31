package Packets.Server;

import Packets.PacketType;

public enum ServerPacketType implements PacketType {
    STATUS,
    JOB_RESULT,
    INFO,
    JOB;

    public int getValue() {
        return this.ordinal();
    }

    public static ServerPacketType getPacketType(int value) {
        return ServerPacketType.values()[value];
    }
}
