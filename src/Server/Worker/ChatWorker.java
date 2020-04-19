package Server.Worker;

import Model.Chat;
import Server.DataManager;

import java.io.*;
import java.util.Date;

public class ChatWorker extends Worker {
    private Chat myChat;

    public ChatWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname, Chat myChat) {
        super(dataManager, clientOut, clientIn, hostname);
        this.myChat = myChat;
    }

    /**
     * Start of the thread. Processes client command to join a chat
     *
     * @see #twoPhaseCommitChat()
     * @see Server.DataManager#chatExists(Chat)
     * @see Server.DataManager#returnChatMessages(Chat)
     * @see Server.DataManager#addChat(Chat)
     * @see Server.DataManager#writeLogEntry(String)
     * @see DataManager#abortChat(Chat)
     * @see Worker#closeClientConnection()
     */
    public void run() {
        if (twoPhaseCommitChat()) {
            myChat.setSuccessful(true);
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + "joined chat " + myChat.getChatId() + " successful");
            dataManager.loginUser(myChat.getUserA(), clientOut);
            dataManager.commitChat(myChat);
        } else {
            myChat.setSuccessful(false);
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + "joined chat " + myChat.getChatId() + " not successful");
        }

        try {
            clientOut.writeObject(myChat);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println("** lost connection to client");
            if(myChat.isSuccessful()) {
                dataManager.abortChat(myChat);
            }
        } finally {
            if (!myChat.isSuccessful()) {
                closeClientConnection();
            } else {
                MessageWorker myMessageWorker = new MessageWorker(dataManager, clientOut, clientIn, hostname);
                Thread t = new Thread(myMessageWorker);
                t.start();
            }
        }
    }

    /**
     * Checks if a chat can be joined with the "Two Phase Commit" protocol
     *
     * @return Success of join
     * @see Worker#openServerConnection()
     * @see Server.DataManager#writeLogEntry(String)
     * @see #sendChatToOtherServer()
     * @see Server.DataManager#chatCanBeCommited(Chat)
     * @see #readChatFromOtherServer()
     * @see Worker#closeServerConnection()
     */
    private boolean twoPhaseCommitChat() {
        if (!openServerConnection()) {
            myChat.setErrorMessage("** connection to server failed");
            return false;
        }

        dataManager.writeLogEntry(new Date() + " - preparing commit for user  " + myChat.getUserA() + " to join chat " + myChat.getChatId());
        myChat.setStatus("PREPARE");

        if (!sendChatToOtherServer()) {
            return false;
        }

        if (!dataManager.chatCanBeCommited(myChat)) {
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + " joining " + myChat.getChatId() + " can not be commited");
            myChat.setStatus("ABORT");
            sendChatToOtherServer();
            closeServerConnection();
            return false;
        }

        if (!readChatFromOtherServer()) {
            return false;
        }

        if (myChat.getStatus().equals("ABORT")) {
            myChat.setStatus("ABORT");
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + " joining " + myChat.getChatId() + " can not be commited");
            sendChatToOtherServer();
            closeServerConnection();
            return false;
        }

        if (myChat.getStatus().equals("READY")) {
            myChat.setStatus("COMMIT");
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + " joining " + myChat.getChatId() + " can be commited");
        }

        if (!sendChatToOtherServer()) {
            return false;
        }

        if (!readChatFromOtherServer()) {
            return false;
        }

        closeServerConnection();

        return myChat.getStatus().equals("OK");
    }

    /**
     * Reads a chat from the other server during two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean readChatFromOtherServer() {
        try {
            myChat = (Chat) serverIn.readObject();
        } catch (IOException e) {
            System.out.println("** lost connection to server");
            myChat.setErrorMessage("** connection to server failed");
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    /**
     * Sends a chat to the other server during two phase commit protocol
     *
     * @return Success of sending
     */
    private boolean sendChatToOtherServer() {
        try {
            serverOut.writeObject(myChat);
            serverOut.flush();
        } catch (IOException e) {
            System.out.println("** lost connection to server");
            myChat.setErrorMessage("** connection to server failed");
            return false;
        }
        return true;
    }


}