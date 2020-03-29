package q2;

import java.util.concurrent.atomic.AtomicStampedReference;

public class q2 {
    static EliminationBackoffStack stack;
    static int numThreads = 4;
    static int maxDelay = 100;
    static int numOps = 50;
    static int timeout = 10;
    static int arraySize = 1;

    public static void main(String[] args) throws InterruptedException {
        stack = new EliminationBackoffStack(timeout, arraySize);
        stack.push(new Node(new AtomicStampedReference<>(1, 0)));
        stack.push(new Node(new AtomicStampedReference<>(2, 0)));
        stack.push(new Node(new AtomicStampedReference<>(3, 0)));

        // Testing ABA problem
        Thread t0 = new Thread(() -> {
            try {
                stack.popDemo();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t1 = new Thread(() -> {
            try {
                Node node3 = stack.pop();
                node3.next = null;
                stack.pop();
                stack.push(node3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t0.start();
        t1.start();

        t0.join();
        t1.join();
    }

    static class StackThread implements Runnable {
        StackThread() {

        }

        @Override
        public void run() {

        }
    }
}
