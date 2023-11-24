package Client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import Packets.Packet;
import java.util.ArrayList;
import java.util.List;

public class RegistrationMoreInfo extends Registration {

    private Map<Long, Packet> jobRequests;
    private Set<Long> waiting;
    private Map<Long, Packet> jobResults;
    private ReentrantLock l;

    public RegistrationMoreInfo(String name, String password, boolean loggedIn) {
        super(name, password, loggedIn);
        this.jobRequests = new HashMap<>();
        this.jobResults = new HashMap<>();
        this.waiting = new HashSet<>();
        this.l = new ReentrantLock();
    }

    public void addJobRequest(Packet packet) {
        try {
            this.l.lock();
            this.jobRequests.put(packet.getId(), packet);
            this.waiting.add(packet.getId());
        } finally {
            this.l.unlock();
        }
    }

    public void addJobResult(Packet packet) {
        try {
            this.l.lock();
            this.jobResults.put(packet.getId(), packet);
            this.waiting.remove(packet.getId());
        } finally {
            this.l.unlock();
        }
    }

    public void addToWaiting(long id) {
        try {
            this.l.lock();
            this.waiting.add(id);
        } finally {
            this.l.unlock();
        }
    }

    public void removeFromWaiting(long id) {
        try {
            this.l.lock();
            this.waiting.remove(id);
        } finally {
            this.l.unlock();
        }
    }

    public List<Packet> getJobRequests() {
        try {
            this.l.lock();
            return this.jobRequests.values().stream().collect(Collectors.toList());
        } finally {
            this.l.unlock();
        }
    }

    public List<Packet> getJobResults() {
        try {
            this.l.lock();
            return this.jobResults.values().stream().collect(Collectors.toList());
        } finally {
            this.l.unlock();
        }
    }

    public List<Long> getWaiting() {
        try {
            this.l.lock();
            return new ArrayList<>(this.waiting);
        } finally {
            this.l.unlock();
        }
    }
}
