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
    public MessageWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname, Message myMessage) {
        super(dataManager, clientOut, clientIn, hostname);
        this.myMessage = myMessage;
        connectionToWriterServer = null;
        writerOut = null;
        dataManager.loginUser(myMessage.getHeader().getSendFrom(), clientOut);
        sendMessage();
    }


    /**
     * Start of the Thread. Endless loop where messages from  a client get processed
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
     * Checks if a message can be send with the "Two Phase Commit" protocol
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
        if(!openServerConnection()){
            return false;
        }

        dataManager.writeLogEntry(new Date() + " - preparing message " + (myMessage.getHeader().getMessageId()) + "for committing");
        myMessage.setStatus("PREPARE");

        if(!sendMessageToOtherServer()) {
            return false;
        }

        if (!dataManager.messageCanBeCommited(myMessage)) {
            dataManager.writeLogEntry(new Date() + " - message " + myMessage.getHeader().getMessageId() + " can not be committed");
            myMessage.setStatus("ABORT");
            sendMessageToOtherServer();
            closeServerConnection();
            return false;
        }

        if(!readMessageFromOtherServer()){
            return false;
        }

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

        if(!sendMessageToOtherServer()) {
            return false;
        }

        if(!readMessageFromOtherServer()){
            return false;
        }

        closeServerConnection();

        return myMessage.getStatus().equals("OK");
    }

    /**
     * Sends message to recipient. Sends the Message to the other server if no output stream to the recipient is known
     *
     * @see Server.DataManager#writeMessage(Message)
     * @see Server.DataManager#getChatPartnerSocket(Message)
     * @see #openServerConnection()
     */
    private void sendMessage() {
        if (twoPhaseCommitMessage()) {
            dataManager.commitMessage(myMessage);

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
                try {
                    writerOut.writeObject(myMessage);
                    writerOut.flush();
                } catch (IOException e) {
                    System.err.println(e);
                } finally {
                    if (connectionToWriterServer != null) {
                        try {
                            connectionToWriterServer.close();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    }
                }
            }
            myMessage.getHeader().setSendSuccessful(true);
        } else {
            myMessage.getHeader().setSendSuccessful(false);
        }

        if(!myMessage.getHeader().isSendSuccessful()){
            try {
                clientOut.writeObject(myMessage);
                clientOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }


    /**
     * Reads a message from the other server during the two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean readMessageFromOtherServer() {
        try {
            myMessage = (Message) serverIn.readObject();
        } catch (IOException e) {
            System.out.println("** lost connection to server");
            myMessage.getHeader().setErrrorMessage("** connection to server failed");
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    /**
     * Sends a message to the other server during the two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean sendMessageToOtherServer() {
        try {
            serverOut.writeObject(myMessage);
            serverOut.flush();
        } catch (IOException e) {
            System.out.println("** lost connection to server");
            myMessage.getHeader().setErrrorMessage("** connection to server failed");
            return false;
        }
        return true;
    }


    /**
     * Starts a connection to the message writer thread of the other server
     *
     * @see MessageWriterWorker
     */
    void openServerConnectionToWriterServer() {
        try {
            connectionToWriterServer = new Socket(InetAddress.getByName(hostname), Integer.parseInt(dataManager.getProperties().getProperty("messageWriterPort")));
            OutputStream outputStream = connectionToWriterServer.getOutputStream();
            writerOut = new ObjectOutputStream(outputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
    }



}