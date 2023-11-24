package Utils;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionQueue<T> {
    public int n_waiting;
    public Queue<T> queue;
    public Condition notEmpty;

    public ConditionQueue(ReentrantLock lock) {
        this.n_waiting = 0;
        this.queue = new ArrayDeque<>();
        this.notEmpty = lock.newCondition();
    }
}