package Client.Worker;


public abstract class Worker implements Runnable {
    String hostname;
    int port;

    public Worker(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }
}
