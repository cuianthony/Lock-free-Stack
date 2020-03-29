package q2;

import java.util.EmptyStackException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EliminationBackoffStack {
    AtomicStampedReference<Node> topRef = new AtomicStampedReference<Node>(new Node(null), 0);
    int exchangeDuration;
    EliminationArray eliminationArray;
    ThreadLocal<RangePolicy> policy;

    /* A lock-free stack of AtomicStampedReferences that avoids the ABA problem.
     * AtomicStampedReference packages together a Node (which has an int value and a
     * next reference to another AtomicStampedReference) and a stamp. The stamp is used
     * to keep track of writes to the Reference by other threads.
     */
    public EliminationBackoffStack(int exchangeDuration, int elimArraySize) {
        this.exchangeDuration = exchangeDuration;
        eliminationArray = new EliminationArray(elimArraySize);
        policy = new ThreadLocal<RangePolicy>() {
            protected synchronized RangePolicy initialValue() {
                return new RangePolicy(elimArraySize);
            }
        };
    }

    private boolean tryPush(Node n) {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        n.next = oldTopNode;
        // Update stamp *not sure if correct
//        n.value.set(n.value.getReference(), n.value.getStamp()+1);
        return (topRef.compareAndSet(oldTopNode, n, stampHolder[0], n.value.getStamp()));
    }

    public void push(Node n) throws InterruptedException {
        RangePolicy rangePolicy = policy.get();
        while (true) {
            if (tryPush(n)) {
                return;
            } else try {
                // If we successfully "push" through an exchange, we should also update stamp
                Node otherNode = eliminationArray.visit(n, rangePolicy.getRange());
                if (otherNode == null) {
                    rangePolicy.recordEliminationSuccess();
                    return;
                }
            } catch (TimeoutException ex) {
                rangePolicy.recordEliminationTimeout();
            }
        }
    }

    private Node tryPop() throws EmptyStackException {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        if (oldTopNode.value.getReference() == null) {
            throw new EmptyStackException();
        }
        Node newTopRef = oldTopNode.next;
        // ISSUE: if newTopRef is null then throws NullPointerException for newTopRef.value
//        if (newTopRef == null && topRef.compareAndSet(oldTopNode, null, stampHolder[0], 0)) {
//            return oldTopNode;
//        } else
        if (topRef.compareAndSet(oldTopNode, newTopRef, stampHolder[0], newTopRef.value.getStamp())) {
            return oldTopNode;
        } else {
            return null;
        }
    }

    public Node pop() throws EmptyStackException, InterruptedException {
        RangePolicy rangePolicy = policy.get();
        while (true) {
            Node returnNode = tryPop();
            if (returnNode != null) {
                return returnNode;
            } else try {
                Node otherNode = eliminationArray.visit(null, rangePolicy.getRange());
                if (otherNode != null) {
                    rangePolicy.recordEliminationSuccess();
                    return otherNode;
                }
            } catch (TimeoutException ex) {
                rangePolicy.recordEliminationTimeout();
            }
        }
    }

    public Node popDemo() throws EmptyStackException, InterruptedException {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        if (oldTopNode == null) {
            throw new EmptyStackException();
        }
        Node newTopRef = oldTopNode.next;
        System.out.println(String.format("Curr TOS has value %d and stamp %d", oldTopNode.value.getReference(), oldTopNode.value.getStamp()));
        // Wait for other thread to do pop, pop, push
        Thread.sleep(5000);
        // this CAS should fail because the stamp on the 3 node will have changed
        if (topRef.compareAndSet(oldTopNode, newTopRef, stampHolder[0], newTopRef.value.getStamp())) {
            return oldTopNode;
        } else {
            System.out.println(String.format("CAS failed: new TOS has value %d and stamp %d", topRef.getReference().value.getReference(), topRef.getStamp()));
            return null;
        }
    }

    public int getSize() {
        int count = 0;
        Node curr = topRef.getReference();
        while (curr.value.getReference() != null) {
            count++;
            curr = curr.next;
        }
        return count;
    }

    public static class RangePolicy {
        int maxRange;
        int currentRange = 1;

        RangePolicy(int maxRange) {
            this.maxRange = maxRange;
        }

        public void recordEliminationSuccess() {
            System.out.println("Successful exchange");
            if (currentRange < maxRange) {
                currentRange++;
            }
        }

        public void recordEliminationTimeout() {
            if (currentRange > 1) {
                currentRange--;
            }
        }

        public int getRange() {
            return currentRange;
        }
    }
}
