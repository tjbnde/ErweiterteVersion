package Server.Worker;

import Model.Message;
import Server.DataManager;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

public class MessageWorker extends Worker {
    private Message myMessage;

    private Socket connectionToWriterServer;
    private ObjectOutputStream writerOut;

    public MessageWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname) {
        super(dataManager, clientOut, clientIn, hostname);
        myMessage = null;
        connectionToWriterServer = null;
        writerOut = null;
    }

    /**
     * Startpunkt des Threads
     * Loggt einen User mit einem zugehörigen Output Stream ein
     * Sendet eine Nachricht an den bestimmten Empfänger
     * Springt in eine Endlos Schleife
     *
     * @see Server.DataManager#loginUser(String, ObjectOutputStream)
     */
    public void run() {
        while (true) {
            try {
                myMessage = (Message) clientIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println(e);
            }
            sendMessage();
        }
    }


    /**
     * Prüft ob die Nachricht erfolgreich versendet werden kann mit Hilfe des "Two Phase Commit" Protokolls
     *
     * @return Wahrheitswert ob die Nachricht erfolgreich versendet werden kann
     * @see Worker#openServerConnection()
     * @see Server.DataManager#writeLogEntry(String)
     * @see #sendMessageToOtherServer()
     * @see Server.DataManager#messageCanBeCommited(Message)
     * @see Worker#closeServerConnection()
     * @see #readMessageFromOtherServer()
     */
    private boolean twoPhaseCommitMessage() {
        openServerConnection();

        dataManager.writeLogEntry(new Date() + " - preparing message " + (myMessage.getHeader().getMessageId()) + "for committing");
        myMessage.setStatus("PREPARE");

        sendMessageToOtherServer();

        if (!dataManager.messageCanBeCommited(myMessage)) {
            dataManager.writeLogEntry(new Date() + " - message " + myMessage.getHeader().getMessageId() + " can not be committed");
            myMessage.setStatus("ABORT");
            sendMessageToOtherServer();
            closeServerConnection();
            return false;
        }

        readMessageFromOtherServer();

        if(myMessage.getStatus().equals("ABORT")) {
            dataManager.writeLogEntry(new Date() + " - set message " + (myMessage.getHeader().getMessageId()) + "ready for aborting");
            sendMessageToOtherServer();
            closeServerConnection();
            return false;
        }

        if (myMessage.getStatus().equals("READY")) {
            dataManager.writeLogEntry(new Date() + " - set message " + (myMessage.getHeader().getMessageId()) + "ready for committing");
            myMessage.setStatus("COMMIT");
        }

        sendMessageToOtherServer();

        readMessageFromOtherServer();

        closeServerConnection();

        return myMessage.getStatus().equals("OK");
    }

    /**
     * Sends message to recipient. Sends the Message to the other server if no output stream to the recipient is known
     *
     * @see Server.DataManager#writeMessage(Message)
     * @see Server.DataManager#getChatPartnerSocket(Message)
     * @see #openServerConnection()
     * @see #sendMessageToWriterServer()
     * @see #closeServerConnectionToWriterServer()
     */
    private void sendMessage() {
        if (twoPhaseCommitMessage()) {
            dataManager.writeMessage(myMessage);
            ObjectOutputStream clientTo = dataManager.getChatPartnerSocket(myMessage);
            if (clientTo != null) {
                try {
                    clientTo.writeObject(myMessage);
                    clientTo.flush();
                } catch (IOException e) {
                    System.err.println(e);
                }
            } else {
                openServerConnectionToWriterServer();
                sendMessageToWriterServer();
                closeServerConnectionToWriterServer();
            }
        }
    }


    /**
     * Liest eine Nachricht vom anderen Server
     */
    private void readMessageFromOtherServer() {
        try {
            myMessage = (Message) serverIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
    }

    /**
     * Sendet eine Nachricht zum anderen Server
     */
    private void sendMessageToOtherServer() {
        try {
            serverOut.writeObject(myMessage);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private void sendMessageToWriterServer() {
        try {
            writerOut.writeObject(myMessage);
            writerOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    void openServerConnectionToWriterServer() {
        try {
            connectionToWriterServer = new Socket(InetAddress.getByName(hostname), Integer.parseInt(dataManager.getProperties().getProperty("messageWriterPort")));
            OutputStream outputStream = serverConnection.getOutputStream();
            writerOut = new ObjectOutputStream(outputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    void closeServerConnectionToWriterServer() {
        if (connectionToWriterServer != null) {
            try {
                connectionToWriterServer.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }


}