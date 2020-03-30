package Server.Worker;

import Model.Message;
import Server.DataManager;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class MessageWorker extends Worker {

    private Message myMessage;
    private ObjectOutputStream clientTo;


    private String hostname;
    private Socket serverConnection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    public MessageWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Message myMessage, String hostname) {
        super(dataManager, clientOut, clientIn);
        this.myMessage = myMessage;
        this.hostname = hostname;
        serverIn = null;
        serverOut = null;
    }


    private boolean twoPhaseCommitMessage() {
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
            if (myMessage.getStatus().equals("READY")) {
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
            if (myMessage.getStatus().equals("OK")) {
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
        return false;

    }


    private void bufferMessage(Message myMessage) {

    }

    private void chatLoop() {
        while (true) {
            try {
                myMessage = (Message) clientIn.readObject();

                // connection to other server for Two-Phase-Commit  Protocol
                serverConnection = new Socket(InetAddress.getByName(hostname), Integer.parseInt(dataManager.getProperties().getProperty("twoPhaseCommitPort")));
                InputStream inputStream = serverConnection.getInputStream();
                serverIn = new ObjectInputStream(inputStream);
                OutputStream outputStream = serverConnection.getOutputStream();
                serverOut = new ObjectOutputStream(outputStream);

                if(twoPhaseCommitMessage()) {
                    dataManager.writeMessage(myMessage);
                    clientTo = getChatPartnerSocket();
                    if (clientTo != null) {
                        clientTo.writeObject(myMessage);
                        clientTo.flush();
                    }
                } else {
                    bufferMessage(myMessage);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            } finally {
                if (serverConnection != null) {
                    try {
                        serverConnection.close();
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            }
        }
    }

    public void run() {
        if(twoPhaseCommitMessage()) {
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
            bufferMessage(myMessage);
        }
        chatLoop();
    }

    private ObjectOutputStream getChatPartnerSocket() {
        String chatPartnerUsername = myMessage.getHeader().getSendTo();
        return dataManager.getLoggedUsers().get(chatPartnerUsername);
    }
}