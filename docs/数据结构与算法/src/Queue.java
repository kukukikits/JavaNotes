public class Queue {
    private int size;
    private Object[] array;
    private int front = 0;
    private int rear = 0;

    public Queue(int size) {
        this.size = size + 1;
        array = new Object[this.size];
    }

    public void clear() {
        front = rear = 0;
    }

    public void enqueue(Object it){
        int slot = (rear + 1) % size;
        if(slot == front) {
            throw new RuntimeException("队列已满");
        }
        rear = slot;
        array[slot] = it;
    }

    public Object dequeue(){
        if(isEmpty()) {
            throw new RuntimeException("队列为空");
        }
        front = (front + 1) % size;
        return array[front];
    }

    public boolean isEmpty() {
        return front == rear;
    }

    public boolean isFull() {
        return (rear + 1) % size == front;
    }

    public static void main(String[] args) {
        Queue queue = new Queue(1);
        System.out.println(queue.isEmpty());
        queue.enqueue(1);
        System.out.println(queue.isEmpty());
        System.out.println(queue.isFull());
        int i = (int)queue.dequeue();
        System.out.println(queue.isEmpty());
        System.out.println(queue.isFull());
        i = (int)queue.dequeue();
    }
}