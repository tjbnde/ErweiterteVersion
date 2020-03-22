import java.net.Socket;

public class Worker extends Thread{
    Socket connection;

    DataManager dataManager;

    public Worker (DataManager dataManager) {
        this.connection = null;
        this.dataManager = dataManager;
    }

    public void run() {

    }

}
