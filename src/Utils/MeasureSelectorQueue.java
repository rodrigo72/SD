package Utils;


// This is a modified queue where elements are added at the end like a regular queue,
// but when polling, elements are removed if they meet a specified condition.
// If an element doesn't meet the condition, the queue is traversed sequentially
// from the first to the last element until a matching element is found or the queue is empty.

public class MeasureSelectorQueue<T extends Measurable> {
    private static class Node<T> {
        public T data;
        public Node<T> prev;
        public Node<T> next;

        public Node(T data) {
            this.data = data;
            this.prev = null;
            this.next = null;
        }
    }

    private transient Node<T> first;
    private transient Node<T> last;
    public transient int length;

    public MeasureSelectorQueue() {
        this.first = null;
        this.last = null;
        this.length = 0;
    }

    public boolean isEmpty() {
        return this.length == 0;
    }

    public int size() {
        return this.length;
    }

    public void add(T data) {
        Node<T> node = new Node<>(data);
        if (this.length == 0) {
            this.first = this.last = node;
        } else {
            node.prev = this.last;
            this.last.next = node;
            this.last = node;
        }
        this.length += 1;
    }

    public T poll(long measure) {
        if (this.length == 0) {
            this.first = this.last = null;
        } else {

            if (this.first.data.measure() <= measure) {
                T data = this.first.data;
                if (this.first != this.last) {
                    this.first.prev = null;
                    this.first = this.first.next;
                } else {
                    this.first = this.last = null;
                }
                this.length -= 1;
                return data;
            }

            Node<T> aux = this.first.next;
            while (aux != null) {
                if (aux.data.measure() <= measure) {
                    if (aux == this.last) {
                        this.last = aux.prev;
                        aux.prev.next = null;
                    } else {
                        aux.next.prev = aux.prev;
                        aux.prev.next = aux.next;
                    }
                    this.length -= 1;
                    return aux.data;
                } else {
                    aux = aux.next;
                }
            }
        }
        return null;
    }
}

