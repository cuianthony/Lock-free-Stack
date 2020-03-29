import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public class DivideRunnable implements Runnable {
    public ArrayDeque<Integer> idStack;
    public char[] s;
    public int left, right;
    public ArrayDeque<q1.Position> posStack;
    public ExecutorService executor;

    DivideRunnable(ArrayDeque<Integer> idStack, char[] s, int left, int right, ArrayDeque<q1.Position> posStack, ExecutorService executor) {
        this.idStack = idStack;
        this.s = s;
        this.left = left;
        this.right = right;
        this.posStack = posStack;
        this.executor = executor;
    }

    @Override
    public void run() {
        int mid = (left+right)/2;
//        System.out.println(String.format("Divide task on %s, l: %d, r: %d, m: %d", new String(s), left, right, mid));

        // Base case: create a new triple
        if (mid == left) {
            ArrayDeque<Integer> newIdStack = idStack.clone();
//            newIdStack.push(id);
            ArrayDeque<q1.Position> newPosStack = posStack.clone();
//            newIdStack.push(id);

            // Create triple based on type of character
            q1.Triple t;
            switch (s[left]) {
                case '[':
                    t = new q1.Triple(newIdStack, newPosStack, false, 1, 1);
                    break;
                case ']':
                    t = new q1.Triple(newIdStack, newPosStack, false, -1, -1);
                    break;
                default:
                    t = new q1.Triple(newIdStack, newPosStack, true, 0, 0);
            }

            System.out.println(String.format("%s made triple %c at %d, mergeID: %d", Thread.currentThread().getName(), s[left], left, t.idStack.peek()));
            q1.Triple mergePair = q1.tripleMap.putIfAbsent(t.idStack.peek(), t); // this will crash with string length 1
            if (mergePair != null) {
//                q1.tripleMap.remove(t.idStack.peek());
                if (mergePair.posStack.peek() == t.posStack.peek()) {
                    System.out.println("CATCH: Positions for this ID are the same, don't merge.");
                    return;
                }
                // If the putIfAbsent does not return null, then the matching triple was already in the map, so merge
                if (mergePair.posStack.peek() == q1.Position.LEFT) {
                    executor.execute(new MergeRunnable(mergePair, t, executor));
                } else {
                    executor.execute(new MergeRunnable(t, mergePair, executor));
                }
            }
            // If putIfAbsent did return null, then our triple will have been inserted atomically
//            if (q1.tripleMap.containsKey(t.idStack.peek())) {
//                // If triple is ready to merge, then give executor new merge task
//                assert t.idStack.peek() != null;
//                q1.Triple newMerge = q1.tripleMap.get(t.idStack.peek());
//                if (newMerge.posStack.peek() == q1.Position.LEFT) {
//                    executor.execute(new MergeRunnable(newMerge, t, executor));
//                } else {
//                    executor.execute(new MergeRunnable(t, newMerge, executor));
//                }
//            } else {
//                // Otherwise, add the triple to the map so its "partner" can initiate the merge
//                assert t.idStack.peek() != null; // If null, then there is only one triple, original string is length 1?
//                q1.tripleMap.put(t.idStack.peek(), t);
//            }

            return;
        }

        // Mark this "position in recursive tree" with an id
        int id = q1.idCounter++;
        // Otherwise, split the char array and create two new DivideRunnables
        DivideRunnable leftDivide = splitString(left, id, mid, q1.Position.LEFT);
        DivideRunnable rightDivide = splitString(mid, id, right, q1.Position.RIGHT);

        executor.execute(leftDivide);
        executor.execute(rightDivide);
    }

    private DivideRunnable splitString(int left, int id, int right, q1.Position p) {
//        System.out.println(String.format("Splitting string %s, l: %d, r: %d, ID: %d, pos: %s", new String(s), left, right, id, p.toString()));
        ArrayDeque<Integer> newIdStack = idStack.clone();
        newIdStack.push(id);
        ArrayDeque<q1.Position> newPosStack = posStack.clone();
        newPosStack.push(p);
        return new DivideRunnable(newIdStack, s, left, right, newPosStack, executor);
    }
}
