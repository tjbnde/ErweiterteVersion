package Model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class MessageHeader implements Serializable {
    private UUID messageId;
    private String sendFrom;
    private String sendTo;
    private Date timeSend;
    private Date expirationTimer;
    private boolean sendSuccessful;
    private int localLamportCounter;

    // header for messages to load from storage
    public MessageHeader(String messageId, String sendFrom, String sendTo, int globalLamportCounter, boolean sendSuccessful, Date timeSend) {
        this.messageId = UUID.fromString(messageId);
        init(sendFrom, sendTo, globalLamportCounter);
        this.timeSend = timeSend;
        expirationTimer = new Date(timeSend.getTime() + 10000);
        this.sendSuccessful = sendSuccessful;
    }

    private void init(String sendFrom, String sendTo, int globalLamportCounter) {
        this.sendTo = sendTo;
        this.sendFrom = sendFrom;
        localLamportCounter = globalLamportCounter;
    }

    // header to create new messages
    public MessageHeader(String sendFrom, String sendTo, int globalLamportCounter ) {
        messageId = UUID.randomUUID();
        init(sendFrom, sendTo, globalLamportCounter);
        long currentTimeMillis = System.currentTimeMillis();
        timeSend = new Date(currentTimeMillis);
        // sets time of expiration 10 seconds after sending
        expirationTimer = new Date(currentTimeMillis + 10000);
        sendSuccessful = false;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getSendFrom() {
        return sendFrom;
    }

    public String getSendTo() {
        return sendTo;
    }

    public Date getTimeSend() {
        return timeSend;
    }

    public Date getExpirationTimer() {
        return expirationTimer;
    }

    public boolean isSendSuccessful() {
        return sendSuccessful;
    }

    public int getLocalLamportCounter() {
        return localLamportCounter;
    }

    public void setSendSuccessful(boolean sendSuccessful) {
        this.sendSuccessful = sendSuccessful;
    }
}
