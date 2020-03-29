package q2;

import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EliminationArray {
    int duration;
    Exchanger<Node>[] exchanger;
    Random random;

    public EliminationArray(int capacity) {
        this.exchanger = (Exchanger<Node>[]) new Exchanger[capacity];
        for (int i=0; i<capacity; i++) {
            exchanger[i] = new Exchanger<Node>();
        }

        random = new Random();
    }

    public Node visit(Node n, int range) throws TimeoutException, InterruptedException {
        int slot = random.nextInt(range);
        // Update stamp if thread is pushing
//        if (n != null) {
//            n.value.set(n.value.getReference(), n.value.getStamp()+1);
//        }
        return (exchanger[slot].exchange(n, duration, TimeUnit.MILLISECONDS));
    }
}
