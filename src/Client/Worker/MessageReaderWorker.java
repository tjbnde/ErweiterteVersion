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

    private Message previousMessage;

    /**
     *
     */
    private Client myClient;

    private boolean exit;

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
        previousMessage = null;
        exit = false;
    }

    /**
     * Endless loop that reads messages from the server and displays them on the console if they are from the current chat
     */
    @Override
    public void run() {
        while (!exit) {
            try {
                myMessage = (Message) serverIn.readObject();
            } catch (IOException e) {
                exit = true;
            } catch (ClassNotFoundException e) {
                System.err.println(e);
            }

            if(previousMessage == null || !previousMessage.getHeader().getMessageId().equals(myMessage.getHeader().getMessageId())) {
                if (myMessage != null) {
                    if (myMessage.getHeader().isSendSuccessful()) {
                        myClient.setGlobalLamportCounter(myMessage.getHeader().getLocalLamportCounter());

                        if (myMessage.getHeader().getSendFrom().equals(myClient.getChat().getUserB()) || myMessage.getHeader().getSendFrom().equals(myClient.getUsername())) {
                            System.out.println();
                            System.out.println(myMessage.getHeader().getSendFrom() + ": ");
                            System.out.println(myMessage.getText());
                            System.out.println();
                        }
                    } else {
                        System.err.println(myMessage.getHeader().getErrrorMessage());
                    }
                    previousMessage = myMessage;
                }
            }
        }

    }
}
