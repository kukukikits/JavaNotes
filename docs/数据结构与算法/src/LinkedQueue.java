public class LinkedQueue {
    
    private Node rear;

    public void enqueue(Object it){
        if(rear == null) {
            rear = new Node(it, null);
            rear.next = rear;
        } else {
            rear.next = new Node(it, rear.next);
            rear = rear.next;
        }
    }

    public Object dequeue() {
        if(rear == null) {
            throw new RuntimeException("队列为空");
        }
        if(rear == rear.next) {
            Object result = rear.value;
            rear = null;
            return result;
        }
        Node front = rear.next;
        Object result = front.value;
        rear.next = front.next;
        return result;
    }

    public void clear() {
        this.rear = null;
    }

    private static class Node {
        private Object value;
        private Node next;
        Node(Object value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    public static void main(String[] args) {
        LinkedQueue queue = new LinkedQueue();
        queue.enqueue(1);
        int i = (int)queue.dequeue();
        System.out.println(i);
        queue.enqueue(1);
        queue.enqueue(2);
        queue.enqueue(3);

        System.out.println(queue.dequeue());
        System.out.println(queue.dequeue());
        System.out.println(queue.dequeue());
    }
}