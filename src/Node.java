
import java.util.concurrent.atomic.AtomicStampedReference;

public class Node {
    public AtomicStampedReference<Integer> value;
    public Node next;

    public Node(Integer value) {
        this.value = new AtomicStampedReference<>(value, 0);
        this.next = null;
    }

    public void updateStamp() {
        this.value.set(this.value.getReference(), this.value.getStamp()+1);
    }
}
