package Client.Worker;

import Model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;

public class MessageReaderWorker implements Runnable {
    /**
     * Object Input Stream from server to read messages
     */
    private ObjectInputStream serverIn;

    /**
     * Message that gets read
     */
    private Message myMessage;

    /**
     * Creates a worker thread that can receive messages from the server
     *
     * @param serverIn - input stream of server
     */
    public MessageReaderWorker(ObjectInputStream serverIn) {
        this.serverIn = serverIn;
        myMessage = null;
    }

    /**
     * Endless loop that reads messages from the server and displays them on the console
     */
    @Override
    public void run() {
        while (true) {
            try {
                myMessage = (Message) serverIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
            System.out.println();
            System.out.println(myMessage.getHeader().getSendFrom() + ": ");
            System.out.println(myMessage.getText());
            System.out.println();
        }

    }
}
