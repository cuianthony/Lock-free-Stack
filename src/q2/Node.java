package q2;

import java.util.concurrent.atomic.AtomicStampedReference;

public class Node {
    public int value;
    public AtomicStampedReference<Node> next;

    public Node(int value) {
        this.value = value;
        this.next = null;
    }
}
