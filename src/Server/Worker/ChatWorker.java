package Server.Worker;

import Model.Chat;
import Model.Message;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ChatWorker extends Worker {
    Chat myChat;

    public ChatWorker(DataManager dataManager, ObjectOutputStream clientOut, ObjectInputStream clientIn, Chat myChat) {
        super(dataManager, clientOut, clientIn);
        this.myChat = myChat;
    }


    public void run() {
        if (myChat.getUserA().equals(myChat.getUserB())) {
            myChat.setErrorMessage("** you can't send messages to yourself");
            try {
                clientOut.writeObject(myChat);
                clientOut.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
            return;
        }
        if (dataManager.userIsRegistered(myChat.getUserB())) {
            myChat.setSucessful(true);
            myChat.setErrorMessage("");
            if (dataManager.chatExists(myChat)) {
                ArrayList<Message> messages = dataManager.returnChatMessages(myChat);
                myChat.setMessages(messages);
            } else {
                dataManager.addChat(myChat);
            }
        } else {
            myChat.setErrorMessage("** user \"" + myChat.getUserB() + "\" is not registered");
        }
        try {
            clientOut.writeObject(myChat);
            clientOut.flush();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            closeConnection();
        }

    }
}