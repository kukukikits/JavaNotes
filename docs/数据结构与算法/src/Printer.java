import java.util.Random;

import sun.misc.Queue;

public class Printer {
    

    public static class ClientServerSimulation {
        private static final int NUMBER_OF_SERVERS = 4;
        private static final double MEAN_INTERARRIVAL_TIME = 20.0;
        private static final int DURATION = 100;
        private static Server[] servers = new Server[NUMBER_OF_SERVERS];
        private static Queue<T> clients = new ArrayQueue();
        private static Random random = new Random(MEAN_INTERARRIVAL_TIME);
        public static void main(String[] args) {
            for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
                servers[i] = new Server();
            }

            int timeofNextArrival = random.nextInt();

            for (int t = 0; t < DURATION; t++) {
                if (t == timeofNextArrival) {
                    clients.enqueue(new Client(t));
                    print(clients);
                    timeofNextArrival += random.nextInt();
                }

                for (int i = 0; i < NUMBER_OF_SERVERS; i++) {
                    if (servers[i].isFree()) {
                        if(!clients.isEmpty()) {
                            servers[i].beginServing((Client)clients.dequeue(), t);
                            print(clients);
                        }
                    } else if(t == servers[i].getTimeServiceEnds()) {
                        servers[i].endServing(t);
                    }
                }
            }
            


        }
    }
}