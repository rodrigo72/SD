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
            case REGISTRATION -> {
                ClientRegistrationPacket registrationPacket = (ClientRegistrationPacket) packet;
                serializeRegistrationPacket(out, registrationPacket);
            }
            case LOGIN -> {
                ClientLoginPacket loginPacket = (ClientLoginPacket) packet;
                serializeLoginPacket(out, loginPacket);
            }
            case LOGOUT -> {
                // No additional data to serialize
            }
            case JOB -> {
                ClientJobPacket jobPacket = (ClientJobPacket) packet;
                serializeJobPacket(out, jobPacket);
            }
            case GET_INFO -> {
                // No additional data to serialize
            }
            default -> {}
        }
    }

    private static void serializeJobPacket(DataOutputStream out, ClientJobPacket packet) throws IOException {
        out.writeLong(packet.getRequiredMemory());
        out.writeInt(packet.getData().length);
        out.write(packet.getData());
    }

    private static void serializeRegistrationPacket(DataOutputStream out, ClientRegistrationPacket packet) throws IOException {
        out.writeUTF(packet.getName());
        out.writeUTF(packet.getPassword());
    }

    private static void serializeLoginPacket(DataOutputStream out, ClientLoginPacket packet) throws IOException {
        out.writeUTF(packet.getName());
        out.writeUTF(packet.getPassword());
    }
}
