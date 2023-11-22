package Packets.Server;

import Packets.Packet;

public class ServerJobResultPacket extends Packet {

    private final ResultStatus status;
    private String errorMessage;
    private byte[] data;

    public enum ResultStatus {
        SUCCESS,
        FAILURE
    }

    public ServerJobResultPacket(long id, String errorMessage) {
        super(id, ServerPacketType.JOB_RESULT);
        this.errorMessage = errorMessage;
        this.status = ResultStatus.FAILURE;
        this.data = null;
    }

    public ServerJobResultPacket(long id, byte[] data) {
        super(id, ServerPacketType.JOB_RESULT);
        this.errorMessage = null;
        this.status = ResultStatus.SUCCESS;
        this.data = data;
    }

    public ResultStatus getStatus() {
        return this.status;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public byte[] getData() {
        return this.data;
    }

    public String toString() {
        return super.toString() + ", " +
                (status == ResultStatus.FAILURE ? errorMessage : data.length + " bytes") + " }";
    }
    
}
