package Client.Worker;

import Model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class MessageReaderWorker implements Runnable{
    private Socket connection;
    private ObjectInputStream serverIn;
    private String username;

    Message myMessage;
    public MessageReaderWorker(ObjectInputStream serverIn, String username) {
        this.username = username;
        connection = null;
        this.serverIn = serverIn;
        myMessage = null;
    }

    @Override
    public void run() {
        try {
            while (true) {
                try {
                    myMessage = (Message) serverIn.readObject();
                    System.out.println(myMessage.getHeader().getSendFrom() + ": ");
                    System.out.println(myMessage.getText());
                } catch (ClassNotFoundException e) {
                    System.err.println(e);
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
