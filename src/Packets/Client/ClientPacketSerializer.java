package Packets.Client;

import java.io.DataOutputStream;
import java.io.IOException;
import Packets.Packet;
import Packets.PacketType;
import Packets.Serializer;

public class ClientPacketSerializer implements Serializer {
    public void serialize(DataOutputStream out, Packet packet) throws IOException {
        PacketType.serialize(packet.getType(), out);
        out.writeLong(packet.getId());

        switch ((ClientPacketType) packet.getType()) {
            case REGISTRATION:
                ClientRegistrationPacket registrationPacket = (ClientRegistrationPacket) packet;
                serializeRegistrationPacket(out, registrationPacket);
                break;
            default:
                break;
        }
    }

    private static void serializeRegistrationPacket(DataOutputStream out, ClientRegistrationPacket packet) throws IOException {
        out.writeUTF(packet.getName());
        out.writeUTF(packet.getPassword());
    }
}
