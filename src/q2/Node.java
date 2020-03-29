package q2;

import java.util.concurrent.atomic.AtomicStampedReference;

public class Node {
    public AtomicStampedReference<Integer> value;
    public Node next;

    public Node(AtomicStampedReference<Integer> value) {
        this.value = value;
        this.next = null;
    }
}
