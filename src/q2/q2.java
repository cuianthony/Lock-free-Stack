package q2;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.Random;

public class q2 {
    static EliminationBackoffStack stack;
    static int numThreads = 16;
    static int maxDelay = 10;
    static int numOps = 1200;
    static int timeout = 2;
    static int arraySize = 4;

    static int popCount = 0;
    static int pushCount = 0;
    static int exchangeCount = 0;

    public static void main(String[] args) throws InterruptedException {
        stack = new EliminationBackoffStack(timeout, arraySize);

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

    synchronized static void incPushCount() {
        pushCount++;
    }
    synchronized static void incPopCount() {
        popCount++;
    }
    synchronized static void incExchangeCount() {
        exchangeCount++;
    }

    static class StackThread implements Runnable {
        int attemptCount = 0;
        Random random = new Random();

        LinkedList<Node> popped = new LinkedList<Node>();

        @Override
        public void run() {
            try {
                while (attemptCount < numOps) {
                    // Choose between attempting a push and a pop
                    int choice = random.nextInt(4);
                    if (choice < 2) {
                        // Push previously popped node
                        if (!popped.isEmpty() && choice == 0) {
                            int nodeIndex = random.nextInt(popped.size());
                            popped.get(nodeIndex).updateStamp(); // increment stamp value to mark this thread is pushing
                            stack.push(popped.get(nodeIndex));

                            // Remove the node we just pushed and replace it with the last node in popped
//                            Node shift = popped.remove(popped.size()-1);
//                            if (!popped.isEmpty() && nodeIndex < popped.size()) {
//                                popped.set(nodeIndex, shift);
//                            }
                            popped.remove(nodeIndex);
                        } else {
                            // Push new node
                            stack.push(new Node(random.nextInt(numOps)));
                        }
                        // Update counts
                        attemptCount++;
                        incPushCount();
                    } else {
                        // Pop
                        try {
                            Node poppedNode = stack.pop();
                            // To represent null, we still have a Node but give it a null Integer value
                            poppedNode.next = new Node(null);
                            if (popped.size() == 20) {
                                popped.remove();
                            }
                            popped.add(poppedNode);
                            incPopCount();
                        } catch (EmptyStackException ignored) { } //do nothing and try again
                        attemptCount++;
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
