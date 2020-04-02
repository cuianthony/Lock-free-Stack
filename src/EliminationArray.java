
import java.util.Random;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EliminationArray {
    int duration;
    Exchanger<Node>[] exchanger;
    Random random;

    public EliminationArray(int duration, int capacity) {
        this.exchanger = (Exchanger<Node>[]) new Exchanger[capacity];
        this.duration = duration;
        for (int i=0; i<capacity; i++) {
            exchanger[i] = new Exchanger<Node>();
        }

        random = new Random();
    }

    public Node visit(Node n, int range) throws TimeoutException, InterruptedException {
        int slot = random.nextInt(range);
        return (exchanger[slot].exchange(n, duration, TimeUnit.MILLISECONDS));
    }
}
