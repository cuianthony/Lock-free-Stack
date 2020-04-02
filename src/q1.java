import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class q1 {
    static int idCounter = 0;
    static ConcurrentHashMap<Integer, Triple> tripleMap;
    static volatile Triple endTriple;

    static int n;
    static int t;
    static long s;

    public static void main(String[] args) {
        if (args.length==2 || args.length==3) {
            n = Integer.parseInt(args[0]);
            t = Integer.parseInt(args[1]);
            s = (args.length>2) ? Integer.parseInt(args[2]) : System.currentTimeMillis();
        }
        else {
            System.out.println("2 or 3 arguments required: n t s(optional)");
            return;
        }

        tripleMap = new ConcurrentHashMap<>(n, (float) 0.75, t);

        // Get the random bracket string
        char[] brackets = Bracket.construct(n, s);
        // Construct the thread pool
        ExecutorService executor = Executors.newFixedThreadPool(t);

        long start = System.currentTimeMillis();
        // Split the string down to base cases by passing tasks to executor
        executor.execute(new DivideRunnable(new ArrayDeque<>(), brackets, 0, brackets.length, new ArrayDeque<>(), executor));

        while (endTriple == null);
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        System.out.println(endTriple.ok + " " + Bracket.verify());
        executor.shutdown();
    }

    synchronized static int incIdCounter() {
        return ++idCounter;
    }

    static class Triple {
        public ArrayDeque<Integer> idStack;
        public ArrayDeque<Position> posStack;
        public boolean ok;
        public int f;
        public int m;

        Triple(ArrayDeque<Integer> idStack, ArrayDeque<Position> posStack, boolean ok, int f, int m) {
            this.idStack = idStack;
            this.posStack = posStack;
            this.ok = ok;
            this.f = f;
            this.m = m;
        }
    }

    enum Position {
        LEFT, RIGHT;
    }
}
