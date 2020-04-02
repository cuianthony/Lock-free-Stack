import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;

public class MergeRunnable implements Runnable {
    public q1.Triple left, right;
    public ExecutorService executor;

    MergeRunnable(q1.Triple left, q1.Triple right, ExecutorService executor) {
        this.left = left;
        this.right = right;
        this.executor = executor;
    }

    @Override
    public void run() {
        // Combine two triples and add the new triple to the map
        boolean newOk = (left.ok && right.ok) ||
                ((left.f + right.f == 0) && (left.m >= 0) && (left.f + right.m >= 0));
        int newF = left.f + right.f;
        int newM = Math.min(left.m, left.f + right.m);

        // Update the position and id stacks
        ArrayDeque<Integer> newIdStack = left.idStack.clone();
        newIdStack.pop();
        ArrayDeque<q1.Position> newPosStack = left.posStack.clone();
        newPosStack.pop();

        q1.Triple merged = new q1.Triple(newIdStack, newPosStack, newOk, newF, newM);

        // End condition: idStack and posStack are both empty - we've come back to top of recursive tree
        if (newIdStack.isEmpty()) {
            // "return" the final Triple
            q1.endTriple = merged;
            return;
        }

        q1.Triple mergePair = q1.tripleMap.putIfAbsent(merged.idStack.peek(), merged); // this will crash with string length 1
        if (mergePair != null) {
            // If the putIfAbsent does not return null, then the matching triple was already in the map, so merge
            if (mergePair.posStack.peek() == q1.Position.LEFT) {
                executor.execute(new MergeRunnable(mergePair, merged, executor));
            } else {
                executor.execute(new MergeRunnable(merged, mergePair, executor));
            }
        }
    }
}
