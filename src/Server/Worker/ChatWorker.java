package Server.Worker;

import Model.Chat;
import Model.Message;
import Server.DataManager;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ChatWorker extends Worker {
    ObjectOutputStream clientOut;
    Chat myChat;

    public ChatWorker(DataManager dataManager, ObjectOutputStream clientOut, Chat myChat) {
        super(dataManager);
        this.clientOut = clientOut;
        this.myChat = myChat;
    }


    public void run() {
        if (myChat.getUserA().equals(myChat.getUserB())) {
            myChat.setErrorMessage("** you can't send messages to yourself");
            try {
                clientOut.writeObject(myChat);
                clientOut.flush();
            } catch(IOException e) {
                System.err.println(e);
            }
            return;
        }
        if(!dataManager.userIsRegistered(myChat.getUserB())) {
            myChat.setErrorMessage("** user \"" + myChat.getUserB() + "\" is not registered" );
            try {
                clientOut.writeObject(myChat);
                clientOut.flush();
            } catch(IOException e) {
                System.err.println(e);
            }
        } else {
            if(dataManager.chatExists(myChat)) {
                ArrayList<Message> messages = dataManager.returnChatMessages(myChat);
                myChat.setMessages(messages);
                try {
                    clientOut.writeObject(myChat);
                    clientOut.flush();
                } catch(IOException e) {
                    System.err.println(e);
                }
            } else {
                dataManager.addChat(myChat);
            }
        }

    }
}
