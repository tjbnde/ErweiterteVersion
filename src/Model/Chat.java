package Model;

import java.io.Serializable;
import java.util.ArrayList;

public class Chat implements Serializable {
    private String chatId;
    private String userA;
    private String userB;
    private String errorMessage;
    private ArrayList<Message> messages;

    public Chat(String userA, String userB) {
        this.userA = userA;
        this.userB = userB;
        chatId = generateChatId();
        errorMessage = "";
        messages = new ArrayList<>();
    }

    public Chat(String chatId, String userA, String userB) {
        this.chatId = chatId;
        this.userA = userA;
        this.userB = userB;
        errorMessage = "";
        messages = new ArrayList<>();
    }

    private String generateChatId() {
        return userA + userB;
    }

    public String getChatId() {
        return chatId;
    }

    public String getUserA() {
        return userA;
    }

    public String getUserB() {
        return userB;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }
}