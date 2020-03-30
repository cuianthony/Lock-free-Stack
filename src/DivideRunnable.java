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
        // Base case: create a new triple
        if (mid == left) {
            ArrayDeque<Integer> newIdStack = idStack.clone();
            ArrayDeque<q1.Position> newPosStack = posStack.clone();

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

            q1.Triple mergePair = q1.tripleMap.putIfAbsent(t.idStack.peek(), t); // this will crash with string length 1
            if (mergePair != null) {
                // If the putIfAbsent does not return null, then the matching triple was already in the map, so merge
                if (mergePair.posStack.peek() == q1.Position.LEFT) {
                    executor.execute(new MergeRunnable(mergePair, t, executor));
                } else {
                    executor.execute(new MergeRunnable(t, mergePair, executor));
                }
            }

            return;
        }

        // Mark this "position in recursive tree" with an id
        int id = q1.incIdCounter();
        // Otherwise, split the char array and create two new DivideRunnables
        DivideRunnable leftDivide = splitString(left, id, mid, q1.Position.LEFT);
        DivideRunnable rightDivide = splitString(mid, id, right, q1.Position.RIGHT);

        executor.execute(leftDivide);
        executor.execute(rightDivide);
    }

    private DivideRunnable splitString(int left, int id, int right, q1.Position p) {
        ArrayDeque<Integer> newIdStack = idStack.clone();
        newIdStack.push(id);
        ArrayDeque<q1.Position> newPosStack = posStack.clone();
        newPosStack.push(p);
        return new DivideRunnable(newIdStack, s, left, right, newPosStack, executor);
    }
}
