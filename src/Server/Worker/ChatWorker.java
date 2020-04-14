package Server.Worker;

import Model.Chat;
import Model.Message;
import Server.DataManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class ChatWorker extends Worker {
    private Chat myChat;

    public ChatWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, String hostname, Chat myChat) {
        super(dataManager, clientOut, clientIn, hostname);
        this.myChat = myChat;
    }

    /**
     * Startpunkt des Threads
     * Prüft ob einem Chat erfolgreich beigetreten werden kann
     * Sendet das Ergebnis an den Client zurück
     *
     * @see #twoPhaseCommitChat()
     * @see Server.DataManager#chatExists(Chat)
     * @see Server.DataManager#returnChatMessages(Chat)
     * @see Server.DataManager#addChat(Chat)
     * @see Server.DataManager#writeLogEntry(String)
     * @see Worker#closeClientConnection()
     */
    public void run() {
        if (twoPhaseCommitChat()) {
            myChat.setSuccessful(true);
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + "joined chat " + myChat.getChatId() + " successful");

            if (dataManager.chatExists(myChat)) {
                ArrayList<Message> messages = dataManager.returnChatMessages(myChat);
                myChat.setMessages(messages);
            } else {
                dataManager.addChat(myChat);
            }
        } else {
            myChat.setSuccessful(false);
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + "joined chat " + myChat.getChatId() + " not successful");
        }

        try {
            clientOut.writeObject(myChat);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeClientConnection();
        }
    }

    /**
     * Prüft ob dem Chat erfolgreich beigetreten werden kann mit Hilfe des "Two Phase Commit" Protokolls
     *
     * @return Wahrheitswert ob dem Chat erfolgreich beigetreten werden kann
     * @see Worker#openServerConnection()
     * @see Server.DataManager#writeLogEntry(String)
     * @see #sendChatToOtherServer()
     * @see Server.DataManager#chatCanBeCommited(Chat)
     * @see #readChatFromOtherServer()
     * @see Worker#closeServerConnection()
     */
    private boolean twoPhaseCommitChat() {
        openServerConnection();

        dataManager.writeLogEntry(new Date() + " - preparing commit for user  " + myChat.getUserA() + " to join chat " + myChat.getChatId());
        myChat.setStatus("PREPARE");
        sendChatToOtherServer();

        if (!dataManager.chatCanBeCommited(myChat)) {
            dataManager.writeLogEntry(new Date() + " - user " + myChat.getUserA() + " joining " + myChat.getChatId() + " can not be commited");
            myChat.setStatus("ABORT");
            sendChatToOtherServer();
            closeServerConnection();
            return false;
        }

        readChatFromOtherServer();

        if(myChat.getStatus().equals("ABORT")) {
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

        sendChatToOtherServer();

        readChatFromOtherServer();

        closeServerConnection();

        return myChat.getStatus().equals("OK");
    }

    /**
     * Liest einen Chat vom anderen Server
     */
    private void readChatFromOtherServer() {
        try {
            myChat = (Chat) serverIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
    }

    /**
     * Sendet einen Chat zum anderen Server
     */
    private void sendChatToOtherServer() {
        try {
            serverOut.writeObject(myChat);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }
    }


}