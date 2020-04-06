package Server.Worker;

import Model.Chat;
import Model.Message;
import Server.DataManager;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class ChatWorker extends Worker {
    Chat myChat;

    private String hostname;
    private Socket serverConnection;
    private ObjectInputStream serverIn;
    private ObjectOutputStream serverOut;

    public ChatWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Chat myChat, String hostname) {
        super(dataManager, clientOut, clientIn);
        this.myChat = myChat;
        this.hostname = hostname;
    }

    private boolean twoPhaseCommitLogin() {
        dataManager.writeLogEntry(new Date() + " - preparing commit for chat " + (myChat.getChatId()));
        myChat.setStatus("PREPARE");
        try {
            serverOut.writeObject(myChat);
            serverOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        }


        if (!dataManager.chatCanBeCommited(myChat)) {
            dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " can not be commited");
            myChat.setStatus("ABORT");
            try {
                serverOut.writeObject(myChat);
                serverOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
            return false;
        }
        try {
            myChat = (Chat) serverIn.readObject();
            if (myChat.getStatus().equals("READY")) {
                myChat.setStatus("COMMIT");
                dataManager.writeLogEntry(new Date() + " - chatr " + myChat.getChatId() + " can be commited");
            } else {
                myChat.setStatus("ABORT");
                dataManager.writeLogEntry(new Date() + " - register for user " + myChat.getChatId() + " can not be commited");
                serverOut.writeObject(myChat);
                serverOut.flush();
                return false;
            }
            serverOut.writeObject(myChat);
            serverOut.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
        try {
            myChat = (Chat) serverIn.readObject();
            if (myChat.getStatus().equals("OK")) {
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
        }
        return false;
    }


    public void run() {
        try {
            serverConnection = new Socket(InetAddress.getByName(hostname), Integer.parseInt(dataManager.getProperties().getProperty("twoPhaseCommitPort")));
            InputStream inputStream = serverConnection.getInputStream();
            serverIn = new ObjectInputStream(inputStream);
            OutputStream outputStream = serverConnection.getOutputStream();
            serverOut = new ObjectOutputStream(outputStream);
        } catch (IOException e) {
            System.err.println(e);
        }
        if (twoPhaseCommitLogin()) {
            myChat.setSuccessful(true);
            myChat.setErrorMessage("");
            if (dataManager.chatExists(myChat)) {
                ArrayList<Message> messages = dataManager.returnChatMessages(myChat);
                myChat.setMessages(messages);
            } else {
                dataManager.addChat(myChat);
            }
        } else {
            myChat.setSuccessful(false);
            dataManager.writeLogEntry(new Date() + " - chat " + myChat.getChatId() + " not successful");
        }

        try {
            clientOut.writeObject(myChat);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeConnection();
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