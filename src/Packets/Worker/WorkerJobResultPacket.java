package Packets.Worker;

import Packets.Packet;

public class WorkerJobResultPacket extends Packet {

    private final String clientName;
    private final ResultStatus status;
    private String errorMessage;
    private byte[] data;

    public enum ResultStatus {
        SUCCESS,
        FAILURE
    }

    public WorkerJobResultPacket(long id, String errorMessage, String clientName) {
        super(id, WorkerPacketType.JOB_RESULT);
        this.clientName = clientName;
        this.errorMessage = errorMessage;
        this.status = ResultStatus.FAILURE;
        this.data = null;
    }

    public WorkerJobResultPacket(long id, byte[] data, String clientName) {
        super(id, WorkerPacketType.JOB_RESULT);
        this.errorMessage = null;
        this.status = ResultStatus.SUCCESS;
        this.clientName = clientName;
        this.data = data;
    }

    public String getClientName() {
        return this.clientName;
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
