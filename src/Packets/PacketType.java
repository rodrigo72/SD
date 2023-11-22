package Packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface PacketType {

    int getValue();

    static void serialize(PacketType type, DataOutputStream out) throws IOException {
        out.writeInt(type.getValue());
    }

    static <T extends Enum<T> & PacketType> T deserialize(DataInputStream in, Class<T> enumType) throws IOException, IllegalArgumentException{
        int value = in.readInt();
        if (value < 0 || value >= enumType.getEnumConstants().length) {
            throw new IllegalArgumentException("Invalid ordinal value for " + enumType.getSimpleName());
        }
        return enumType.getEnumConstants()[value];
    }
}
