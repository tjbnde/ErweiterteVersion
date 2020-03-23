package Server.Worker;

import Model.Message;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectOutputStream;

public class MessageWorker extends Worker {
    private ObjectOutputStream clientOut;

    private Message myMessage;

    public MessageWorker(DataManager dataManager, Message myMessage) {
        super(dataManager);
        this.clientOut = clientOut;
        this.myMessage = myMessage;
    }

    public void run() {

        try {
            clientOut.writeObject(myMessage);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}