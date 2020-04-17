package Client.Worker;

import Client.Client;
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
     *
     */
    private Client myClient;

    /**
     * Creates a worker thread that can receive messages from the server
     *
     * @param myClient - client which receives messages
     * @param serverIn - input stream of server
     */
    public MessageReaderWorker(ObjectInputStream serverIn, Client myClient) {
        this.serverIn = serverIn;
        this.myClient = myClient;
        myMessage = null;
    }

    /**
     * Endless loop that reads messages from the server and displays them on the console if they are from the current chat
     */
    @Override
    public void run() {
        while (true) {
            try {
                myMessage = (Message) serverIn.readObject();
                myClient.setGlobalLamportCounter(myMessage.getHeader().getLocalLamportCounter());
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
            if(myMessage.getHeader().getSendFrom().equals(myClient.getChat().getUserB())) {
                System.out.println();
                System.out.println(myMessage.getHeader().getSendFrom() + ": ");
                System.out.println(myMessage.getText());
                System.out.println();
            }
        }

    }
}
