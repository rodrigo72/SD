package Packets.Client;

import Packets.PacketType;


public enum ClientPacketType implements PacketType {
    REGISTRATION,
    LOGIN,
    LOGOUT,
    JOB,
    GET_INFO;

    public int getValue() {
        return this.ordinal();
    }

    public static ClientPacketType getPacketType(int value) {
        return ClientPacketType.values()[value];
    }

}
