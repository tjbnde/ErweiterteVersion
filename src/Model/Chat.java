package Model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Model Klasse Chat
 * Implementiert Serializable damit Instanzen dieser Klasse
 * über ObjectInputStream & ObjectOutputStream versendet und gelesen werden können
 */
public class Chat implements Serializable {
    private String chatId;
    private String userA;
    private String userB;
    private String errorMessage;
    private String status;
    private ArrayList<Message> messages;
    private boolean successful;

    public Chat(String userA, String userB) {
        this.userA = userA;
        this.userB = userB;
        chatId = generateChatId();
        errorMessage = "";
        messages = new ArrayList<>();
        successful = false;
    }

    public Chat(String chatId, String userA, String userB) {
        this.chatId = chatId;
        this.userA = userA;
        this.userB = userB;
        errorMessage = "";
        messages = new ArrayList<>();
        successful = false;
    }

    // generates unique chatId, as every username is unique
    private String generateChatId() {
        return userA + userB;
    }


    // Getter & Setter

    public String getChatId() {
        return chatId.toString();
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

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}