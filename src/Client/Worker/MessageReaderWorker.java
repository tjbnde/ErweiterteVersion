package Client.Worker;

import Model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;

public class MessageReaderWorker implements Runnable{
    private ObjectInputStream serverIn;
    private Message myMessage;

    public MessageReaderWorker(ObjectInputStream serverIn) {
        this.serverIn = serverIn;
        myMessage = null;
    }

    @Override
    public void run() {
        try {
            while (true) {
                try {
                    myMessage = (Message) serverIn.readObject();
                    System.out.println();
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
