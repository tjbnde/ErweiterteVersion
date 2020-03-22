import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MessageWorker extends Worker {
    ObjectInputStream networkIn;
    ObjectOutputStream networkOut;

    Message message;

    public MessageWorker( DataManager dataManager) {
        super(dataManager);
        message = null;
    }

    public void run() {
        while(true) {

            try {
                networkOut = new ObjectOutputStream(connection.getOutputStream());
                networkIn = new ObjectInputStream(connection.getInputStream());

                Message myMessage = (Message) networkIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }
}
