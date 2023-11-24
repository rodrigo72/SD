package Server;

import Packets.Packet;

public class ServerWorker implements Runnable {

    private final SharedState sharedState;
    private boolean running = false;

    public ServerWorker(SharedState sharedState) {
        this.sharedState = sharedState;
    }

    @Override
    public void run() {
        this.running = true;
        while (this.running) {

            Job job = this.sharedState.dequeueJob();
            if (job == null)
                continue;

            Packet packet = job.run();
            System.out.println("Finished job, sending packet to thread " + job.getThreadId() + ".");
            this.sharedState.sendJobResult(job.getThreadId(), packet);
            System.out.println("Packet sent to thread");
        }
    }

    public void stop() {
        this.running = false;
    }
    
}