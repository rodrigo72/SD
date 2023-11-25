package Server;

import sd23.*;
import Packets.Server.ServerJobResultPacket;
import Packets.Packet;
import Utils.Measurable;

public class Job implements Measurable {

    private final long requiredMemory;
    private final long id;
    private final long threadId;
    private final byte[] data;
    private final String clientName;

    public Job(String clientName, long requiredMemory, long id, byte[] data, long threadId) {
        this.requiredMemory = requiredMemory;
        this.id = id;
        this.data = data;
        this.clientName = clientName;
        this.threadId = threadId;
    }

    public Packet run() {
        Packet resultPacket = null;
        try {
            byte[] output = JobFunction.execute(data);
            resultPacket = new ServerJobResultPacket(id, output);
        } catch (JobFunctionException e) {
            resultPacket = new ServerJobResultPacket(id, e.getMessage());
        }
        return resultPacket;
    }

    public String getClientName() {
        return this.clientName;
    }

    public long getRequiredMemory() {
        return this.requiredMemory;
    }

    public long getId() {
        return this.id;
    }

    public long getThreadId() {
        return this.threadId;
    }

    public long measure() {
        return this.requiredMemory;
    }
}