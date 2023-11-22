package Packets;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Serializer {
    void serialize(DataOutputStream out, Packet packet) throws IOException;
}
