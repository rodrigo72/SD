package Utils;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// NOT USED (yet)
public class LamportClock {
    private long timestamp;
    private final ReadWriteLock l;

    public LamportClock() {
        this.timestamp = 0L;
        this.l = new ReentrantReadWriteLock();
    }

    public LamportClock(long init) {
        this.timestamp = init;
        this.l = new ReentrantReadWriteLock();
    }

    public void increment() {
        try {
            this.l.writeLock().lock();
            this.timestamp += 1;
        } finally {
            this.l.writeLock().unlock();
        }
    }

    public void compareAndUpdate(long receivedTimestamp) {
        try {
            this.l.writeLock().lock();
            if (receivedTimestamp > this.timestamp)
                this.timestamp = receivedTimestamp;
            this.timestamp += 1;
        } finally {
            this.l.writeLock().unlock();
        }
    }

    public long getTime() {
        try {
            this.l.readLock().lock();
            return this.timestamp;
        } finally {
            this.l.readLock().unlock();
        }
    }

}
