import com.sun.scenario.effect.Merge;

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


        System.out.println(String.format("%s merged triples %s:%d and %s:%d", Thread.currentThread().getName(), left.posStack.peek().toString(), left.idStack.peek(),
                right.posStack.peek().toString(), right.idStack.peek()));

        // SOURCE OF CRASHES:
        assert left.posStack.peek() != right.posStack.peek();

        // End condition: idStack and posStack are both empty - we've come back to top of recursive tree
        if (newIdStack.isEmpty()) {
            // "return" the final Triple
            q1.endTriple = merged;
            return;
        }

        q1.Triple mergePair = q1.tripleMap.putIfAbsent(merged.idStack.peek(), merged); // this will crash with string length 1
        if (mergePair != null) {
//            q1.tripleMap.remove(merged.idStack.peek());
            if (mergePair.posStack.peek() == merged.posStack.peek()) {
                System.out.println("CATCH: Positions for this ID are the same, don't merge.");
                return;
            }
            // If the putIfAbsent does not return null, then the matching triple was already in the map, so merge
            if (mergePair.posStack.peek() == q1.Position.LEFT) {
                executor.execute(new MergeRunnable(mergePair, merged, executor));
            } else {
                executor.execute(new MergeRunnable(merged, mergePair, executor));
            }
        }
//        if (q1.tripleMap.containsKey(merged.idStack.peek())) {
//            // If triple is ready to merge, then give executor new merge task
//            // Need to determine which is the left and which is the right
//            assert merged.idStack.peek() != null;
//            q1.Triple newMerge = q1.tripleMap.get(merged.idStack.peek());
//            if (newMerge.posStack.peek() == q1.Position.LEFT) {
//                executor.execute(new MergeRunnable(newMerge, merged, executor));
//            } else {
//                executor.execute(new MergeRunnable(merged, newMerge, executor));
//            }
//        } else {
//            // Otherwise, add the triple to the map so its "partner" can initiate the merge
//            assert merged.idStack.peek() != null; // If null, then there is only one triple, original string is length 1?
//            q1.tripleMap.put(merged.idStack.peek(), merged);
//        }
    }
}
