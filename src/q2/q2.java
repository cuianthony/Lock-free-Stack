package q2;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicStampedReference;

public class q2 {
    static EliminationBackoffStack stack;
    static int numThreads = 8;
    static int maxDelay = 50;
    static int numOps = 200;
    static int timeout = 0;
    static int arraySize = 1;

    static volatile int popCount = 0;
    static volatile int pushCount = 0;
    static volatile int exchangeCount = 0;

    public static void main(String[] args) throws InterruptedException {
        stack = new EliminationBackoffStack(timeout, arraySize);
//        stack.push(new Node(1));
//        stack.push(new Node(2));
//        stack.push(new Node(3));

        Thread[] threads = new Thread[numThreads];
        for (int i=0; i<numThreads; i++) {
            threads[i] = new Thread(new StackThread());
        }

        long start = System.currentTimeMillis();
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);

        System.out.println("Num exchanges: " + exchangeCount);
        System.out.println(pushCount + " " + popCount + " " + stack.getSize());
    }

    static class StackThread implements Runnable {
//        int popCount = 0;
//        int pushCount = 0;
        int attemptCount = 0;
        Random random = new Random();

        ArrayList<Node> popped = new ArrayList<Node>();

        StackThread() {

        }

        @Override
        public void run() {
            try {
                while (attemptCount < numOps) {
                    // Choose between attempting a push and a pop
                    int choice = random.nextInt(4);
                    if (choice < 2) {
                        // Push previously popped node
                        if (!popped.isEmpty() && choice == 0) {
//                            System.out.println(String.format("%s old push attempt", Thread.currentThread().getName()));
                            int nodeIndex = random.nextInt(popped.size());
                            popped.get(nodeIndex).updateStamp(); // increment stamp value to mark this thread is pushing
                            stack.push(popped.get(nodeIndex));

                            // Remove the node we just pushed and replace it with the last node in popped
                            Node shift = popped.remove(popped.size()-1);
                            if (!popped.isEmpty() && nodeIndex < popped.size()) {
                                popped.set(nodeIndex, shift);
                            }
//                            popped.set(nodeIndex, popped.remove(popped.size() - 1));
                        } else {
                            // Push new node
//                            System.out.println(String.format("%s new push attempt", Thread.currentThread().getName()));
                            stack.push(new Node(random.nextInt(numOps)));
                        }
                        // Update counts
                        attemptCount++;
                        pushCount++;
                    } else {
//                        System.out.println(String.format("%s pop attempt", Thread.currentThread().getName()));
                        // Pop
                        try {
                            Node poppedNode = stack.pop();
                            // To represent null, we still have a Node but give it a null Integer value
                            poppedNode.next = new Node(null);
                            if (popped.size() == 20) {
                                popped.remove(0);
                            }
                            popped.add(poppedNode);
                            popCount++;
                            attemptCount++;
                        } catch (EmptyStackException e) {
                            attemptCount++;
                        }
                    }
                    // Random sleep
                    Thread.sleep(random.nextInt(maxDelay));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void basicABATest() throws InterruptedException {
        // Testing ABA problem with 2 threads
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
}
