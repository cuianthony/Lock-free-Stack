package q2;

import java.sql.Time;
import java.util.EmptyStackException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EliminationBackoffStack {
    AtomicStampedReference<Node> topRef = new AtomicStampedReference<Node>(null, 0);
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

    private boolean tryPush(AtomicStampedReference<Node> nodeRef) {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        nodeRef.getReference().next = oldTopRef;
        // Update stamp *not sure if correct
        nodeRef.set(nodeRef.getReference(), nodeRef.getStamp()+1);
        return (topRef.compareAndSet(oldTopNode, nodeRef.getReference(), stampHolder[0], nodeRef.getStamp()));
    }

    public void push(AtomicStampedReference<Node> nodeRef) throws InterruptedException {
        RangePolicy rangePolicy = policy.get();
        while (true) {
            if (tryPush(nodeRef)) {
                return;
            } else try {
                // If we successfully "push" through an exchange, we should also update stamp
                AtomicStampedReference<Node> otherRef = eliminationArray.visit(nodeRef, rangePolicy.getRange());
                if (otherRef == null) {
                    rangePolicy.recordEliminationSuccess();
                    return;
                }
            } catch (TimeoutException ex) {
                rangePolicy.recordEliminationTimeout();
            }
        }
    }

    private AtomicStampedReference<Node> tryPop() throws EmptyStackException {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        if (oldTopNode == null) {
            throw new EmptyStackException();
        }
        AtomicStampedReference<Node> newTopRef = oldTopNode.next;
        if (topRef.compareAndSet(oldTopNode, newTopRef.getReference(), stampHolder[0], newTopRef.getStamp())) {
            return oldTopRef;
        } else {
            return new AtomicStampedReference<Node>(null, 0);
        }
    }

    public AtomicStampedReference<Node> pop() throws EmptyStackException, InterruptedException {
        RangePolicy rangePolicy = policy.get();
        while (true) {
            AtomicStampedReference<Node> returnRef = tryPop();
            if (returnRef.getReference() != null) {
                return returnRef;
            } else try {
                AtomicStampedReference<Node> otherRef = eliminationArray.visit(null, rangePolicy.getRange());
                if (otherRef != null) {
                    rangePolicy.recordEliminationSuccess();
                    return returnRef;
                }
            } catch (TimeoutException ex) {
                rangePolicy.recordEliminationTimeout();
            }
        }
    }

    public AtomicStampedReference<Node> popDemo() throws EmptyStackException, InterruptedException {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = topRef.get(stampHolder);
        if (oldTopNode == null) {
            throw new EmptyStackException();
        }
        AtomicStampedReference<Node> newTopRef = oldTopNode.next;
        System.out.println(String.format("Curr TOS has value %d and stamp %d", oldTopRef.getReference().value, oldTopRef.getStamp()));
        // Wait for other thread to do pop, pop, push
        Thread.sleep(5000);
        // this CAS should fail because the stamp on the 3 node will have changed
        if (topRef.compareAndSet(oldTopNode, newTopRef.getReference(), stampHolder[0], newTopRef.getStamp())) {
            return oldTopRef;
        } else {
            System.out.println(String.format("CAS failed: new TOS has value %d and stamp %d", topRef.getReference().value, topRef.getStamp()));
            return new AtomicStampedReference<Node>(null, 0);
        }
    }

    public static class RangePolicy {
        int maxRange;
        int currentRange = 1;

        RangePolicy(int maxRange) {
            this.maxRange = maxRange;
        }

        public void recordEliminationSuccess() {
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
