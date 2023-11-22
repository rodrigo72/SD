package Packets.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import Packets.Packet;
import Packets.PacketType;
import Packets.Serializer;

public class ServerPacketSerializer implements Serializer {
    public void serialize(DataOutputStream out, Packet packet) throws IOException {
        PacketType.serialize(packet.getType(), out);
        out.writeLong(packet.getId());

        switch ((ServerPacketType) packet.getType()) {
            case STATUS:
                ServerStatusPacket statusPacket = (ServerStatusPacket) packet;
                serializeStatusPacket(out, statusPacket);
                break;
            default:
                break;
        }
    }

    private static void serializeStatusPacket(DataOutputStream out, ServerStatusPacket packet) throws IOException {
        out.writeInt(packet.getStatus().ordinal());
    }
}
