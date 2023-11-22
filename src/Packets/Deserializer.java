package Packets;

import java.io.DataInputStream;
import java.io.IOException;

public interface Deserializer {
    Packet deserialize(DataInputStream in) throws IOException;   
}
