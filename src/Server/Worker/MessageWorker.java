package Server.Worker;

import Model.Message;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MessageWorker extends Worker {

    private Message myMessage;
    private ObjectOutputStream clientTo;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    public MessageWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Message myMessage, ObjectInputStream serverIn, ObjectOutputStream serverOut) {
        super(dataManager, clientOut, clientIn);
        this.myMessage = myMessage;
        this.serverIn = serverIn;
        this.serverOut = serverOut;
    }



    private boolean committingMessage() {
        dataManager.writeLogEntry(System.currentTimeMillis() + " - preparing message " + (myMessage.getHeader().getMessageId()) + "for committing");
        myMessage.setStatus("PREPARE");
        try {
            serverOut.writeObject(myMessage);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }

        try {
            myMessage = (Message) serverIn.readObject();
            if(myMessage.getStatus().equals("READY")) {
                dataManager.writeLogEntry(System.currentTimeMillis() + " - set message " + (myMessage.getHeader().getMessageId()) + "ready for committing");
                myMessage.setStatus("COMMIT");
            } else {
                dataManager.writeLogEntry(System.currentTimeMillis() + " - set message " + (myMessage.getHeader().getMessageId()) + "ready for aborting");
                myMessage.setStatus("ABORT");
                serverOut.writeObject(myMessage);
                serverOut.flush();
                return false;
            }
            serverOut.writeObject(myMessage);
            serverOut.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }

        try {
            myMessage = (Message) serverIn.readObject();
            if(myMessage.getStatus().equals("OK")) {
                return true;
            }
        } catch(IOException | ClassNotFoundException e) {
                System.err.println(e);
        }
        return false;

    }


    private void bufferMessage() {

    }

    private void chatLoop() {
        while (true) {
            if(committingMessage()) {
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
            } else {
                bufferMessage();
            }

        }
    }

    public void run() {
        if (committingMessage()) {
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
        } else {
            bufferMessage();
        }
        chatLoop();
    }

    private ObjectOutputStream getChatPartnerSocket() {
        String chatPartnerUsername = myMessage.getHeader().getSendTo();
        return dataManager.getLoggedUsers().get(chatPartnerUsername);
    }
}