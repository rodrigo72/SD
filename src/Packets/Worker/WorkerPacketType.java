package Packets.Worker;

import Packets.PacketType;

public enum WorkerPacketType implements PacketType {
    CONNECTION,
    JOB_RESULT,
    DISCONNECTION;
    
    public int getValue() {
        return this.ordinal();
    }

    public static WorkerPacketType getPacketType(int value) {
        return WorkerPacketType.values()[value];
    }
}
