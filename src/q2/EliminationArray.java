package q2;

import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EliminationArray {
    int duration;
    Exchanger<AtomicStampedReference<Node>>[] exchanger;
    Random random;

    public EliminationArray(int capacity) {
        this.exchanger = (Exchanger<AtomicStampedReference<Node>>[]) new Exchanger[capacity];
        for (int i=0; i<capacity; i++) {
            exchanger[i] = new Exchanger<>();
        }

        random = new Random();
    }

    public AtomicStampedReference<Node> visit(AtomicStampedReference<Node> nodeRef, int range) throws TimeoutException, InterruptedException {
        int slot = random.nextInt(range);
        // Update stamp if thread is pushing
        if (nodeRef != null) {
            nodeRef.set(nodeRef.getReference(), nodeRef.getStamp()+1);
        }
        return (exchanger[slot].exchange(nodeRef, duration, TimeUnit.MILLISECONDS));
    }
}
