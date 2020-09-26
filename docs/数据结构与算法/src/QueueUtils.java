import java.util.ArrayDeque;
import java.util.Queue;

public class QueueUtils {
    public static Queue merge(Queue q1, Queue q2) {
        int size = q1.size() + q2.size();
        ArrayDeque result = new ArrayDeque<>(size);
        boolean q1NotEmpty = false, q2NotEmpty = false;
        while((q1NotEmpty = !q1.isEmpty()) | (q2NotEmpty = !q2.isEmpty())) {
            if(q1NotEmpty) {
                result.add(q1.remove());
            }
            if(q2NotEmpty) {
                result.add(q2.remove());
            }
        }
        return result;
    }

    public static void main(String[] args) {
        ArrayDeque q1 = new ArrayDeque<>();
        ArrayDeque q2 = new ArrayDeque<>();
        for (int i = 0; i < 10; i++) {
            q1.add(i);
            q2.add(i);
        }
        Queue q = merge(q1, q2);
        while(!q.isEmpty()) {
            System.out.println(q.remove());
        }
    }
}