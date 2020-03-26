package Server.Worker;

import Model.Message;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MessageWorker extends Worker {

    private Message myMessage;
    private ObjectOutputStream clientTo;

    public MessageWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Message myMessage) {
        super(dataManager, clientOut, clientIn);
        this.myMessage = myMessage;
    }

    public void run() {
        dataManager.loginUser(myMessage.getHeader().getSendFrom(), clientOut);
        dataManager.writeMessage(myMessage);
        clientTo = getChatPartnerSocket();
        if (clientTo != null) {
            try {
                clientTo.writeObject(myMessage);
                clientTo.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        while(true) {
            try {
                myMessage = (Message) clientIn.readObject();
                dataManager.writeMessage(myMessage);
                clientTo = getChatPartnerSocket();
                if (clientTo != null) {
                    clientTo.writeObject(myMessage);
                    clientTo.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
        }
    }

    private ObjectOutputStream getChatPartnerSocket() {
        String chatPartnerUsername = myMessage.getHeader().getSendTo();
        return dataManager.getLoggedUsers().get(chatPartnerUsername);
    }
}