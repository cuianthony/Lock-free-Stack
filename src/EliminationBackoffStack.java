
import java.util.EmptyStackException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicStampedReference;

public class EliminationBackoffStack {
    AtomicStampedReference<Node> topRef = new AtomicStampedReference<Node>(new Node(null), 0);
    EliminationArray eliminationArray;
    ThreadLocal<RangePolicy> policy;

    /* A lock-free stack of AtomicStampedReferences that avoids the ABA problem.
     * AtomicStampedReference packages together a Node and a stamp. The stamp is used
     * to keep track of writes to the Reference by other threads.
     * The Node itself uses an AtomicStampedReference with an Integer value and a stamp.
     * This stamp and the one stored on the stack are the same value.
     * This implementation follows the EliminationBackoffStack demonstrated in the textbook,
     * using a dynamic range policy for the elimination array.
     */
    public EliminationBackoffStack(int exchangeDuration, int elimArraySize) {
//        this.exchangeDuration = exchangeDuration;
        eliminationArray = new EliminationArray(exchangeDuration, elimArraySize);
        policy = new ThreadLocal<RangePolicy>() {
            protected synchronized RangePolicy initialValue() {
                return new RangePolicy(elimArraySize);
            }
        };
    }

    // Attempt to push the node onto the stack. If successful, returns true. Else, returns false.
    private boolean tryPush(Node n) {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        n.next = oldTopNode;
        return (topRef.compareAndSet(oldTopNode, n, stampHolder[0], n.value.getStamp()));
    }

    // Push process. First tries to push the node. If the push fails, then attempts to exchange with
    // another thread in the elimination array. If exchange is successful, returns. Else, repeat the
    // process of trying to push the node.
    public void push(Node n) throws InterruptedException {
        RangePolicy rangePolicy = policy.get();
        n.updateStamp(); // increment stamp value to mark this thread is pushing
        while (true) {
            if (tryPush(n)) {
                return;
            } else try {
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

    // Attempt to pop a node from the stack. If successful, returns the popped node. If the stack is
    // empty, throws an EmptyStackException. If the CAS fails otherwise, return null.
    private Node tryPop() throws EmptyStackException {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        if (oldTopNode.value.getReference() == null) { //stack is empty
            throw new EmptyStackException(); // immediately exit and try a new stack operation
        }
        Node newTopRef = oldTopNode.next;
        if (topRef.compareAndSet(oldTopNode, newTopRef, stampHolder[0], newTopRef.value.getStamp())) {
            return oldTopNode;
        } else {
            return null;
        }
    }

    // Pop process. First tries to pop the node. If the pop fails, then tries to exchange with another
    // thread in the elimination array. If this succeeds, return the exchanged node. Otherwise, loop back
    // and repeat the pop process.
    // This method propagates an EmptyStackException up, allowing the thread to attempt a new stack
    // operation.
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

    // Used for basicABATest
    public Node popDemo() throws EmptyStackException, InterruptedException {
        AtomicStampedReference<Node> oldTopRef = topRef;
        int[] stampHolder = new int[1];
        Node oldTopNode = oldTopRef.get(stampHolder);
        if (oldTopNode.value.getReference() == null) { //stack is empty
            throw new EmptyStackException(); // immediately exit and try a new stack operation
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

    // Returns size of the stack
    public int getSize() {
        int count = 0;
        Node curr = topRef.getReference();
        while (curr.value.getReference() != null) {
            count++;
            curr = curr.next;
        }
        return count;
    }

    // Used for optimally selecting an Exchanger from the elimination array
    public static class RangePolicy {
        int maxRange;
        int currentRange = 1;

        RangePolicy(int maxRange) {
            this.maxRange = maxRange;
        }

        public void recordEliminationSuccess() {
//            q2.incExchangeCount();
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
