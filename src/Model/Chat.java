package Model;

import java.io.Serializable;
import java.util.ArrayList;

public class Chat implements Serializable {
    // unique chat id
    private String chatId;

    // username of first user
    private String userA;

    // username of second user
    private String userB;
    private String errorMessage;

    // status for two phase commit protocol
    private String status;

    // messages of the chat
    private ArrayList<Message> messages;

    private boolean successful;

    private boolean newCreated;

    public Chat(String userA, String userB) {
        this.userA = userA;
        this.userB = userB;
        chatId = generateChatId();
        errorMessage = "";
        messages = new ArrayList<>();
        successful = false;
        newCreated = false;
    }

    public Chat(String chatId, String userA, String userB) {
        this.chatId = chatId;
        this.userA = userA;
        this.userB = userB;
        errorMessage = "";
        messages = new ArrayList<>();
        successful = false;
        newCreated = false;
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

    public boolean isNewCreated() {
        return newCreated;
    }

    public void setNewCreated(boolean newCreated) {
        this.newCreated = newCreated;
    }
}