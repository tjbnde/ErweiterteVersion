package Client.Worker;

import Model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *
 */
public class MessageReaderWorker implements Runnable {
    private ObjectInputStream serverIn;
    private Message myMessage;

    /**
     * Konstrukter der Klasse
     *
     * @param serverIn ObjectInputStream um Nachrichten vom Server erhalten zu können
     */
    public MessageReaderWorker(ObjectInputStream serverIn) {
        this.serverIn = serverIn;
        myMessage = null;
    }

    /**
     * Endlos Schleife, in der Nachrichten vom Server abgefragt werden
     * und anschließend auf der Konsole ausgegeben werden
     */
    @Override
    public void run() {
        // Endlos-Schleife, die permanent Nachrichten vom Server abfrägt
        // und diese anschließend auf der Konsole ausgibt
        while (true) {
            try {
                myMessage = (Message) serverIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
            System.out.println();
            System.out.println(myMessage.getHeader().getSendFrom() + ": ");
            System.out.println(myMessage.getText());
        }

    }
}
