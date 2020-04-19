package Model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class MessageHeader implements Serializable {
    // unique message id
    private UUID messageId;

    // username of sender
    private String sendFrom;

    // username of recipient
    private String sendTo;

    // timestamp of message
    private Date timeSend;

    // expiration time of message
    private Date expirationTimer;

    private boolean sendSuccessful;

    private String errrorMessage;

    // lamport counter for logical order of messages
    private int localLamportCounter;

    /**
     * Konstrukter um eine Nachricht aus dem Speicher zu laden
     * @param messageId Eindeutige ID einer Nachricht
     * @param sendFrom Username des Absenders
     * @param sendTo Username des Empfängers
     * @param globalLamportCounter LamportCounter der Nachricht um sie in einer korrekten Reihenfolge anzuzeigen
     * @param sendSuccessful Wahrheitswert ob die Nachricht erfolgreich versendet wurde
     * @param timeSend Zeitstempel der Nachricht
     */
    public MessageHeader(String messageId, String sendFrom, String sendTo, int globalLamportCounter, boolean sendSuccessful, Date timeSend) {
        this.messageId = UUID.fromString(messageId);
        this.timeSend = timeSend;
        this.sendSuccessful = sendSuccessful;
        init(sendFrom, sendTo, globalLamportCounter);

    }

    // header to create new messages

    /**
     * Konstrukter um eine neue Nachricht zu erstellen
     * @param sendFrom Username des Absenders
     * @param sendTo Username des Empfängers
     * @param globalLamportCounter Aktueller Wert des Lamport Counters
     */
    public MessageHeader(String sendFrom, String sendTo, int globalLamportCounter ) {
        messageId = UUID.randomUUID();
        long currentTimeMillis = System.currentTimeMillis();
        timeSend = new Date(currentTimeMillis);
        sendSuccessful = false;
        init(sendFrom, sendTo, globalLamportCounter + 1);
    }

    private void init(String sendFrom, String sendTo, int globalLamportCounter) {
        this.sendTo = sendTo;
        this.sendFrom = sendFrom;
        localLamportCounter = globalLamportCounter;
        // sets time of expiration 10 seconds after sending
        expirationTimer = new Date(timeSend.getTime() + 10000);
    }

    // Getter & Setter

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

    public void setSendSuccessful(boolean sendSuccessful) {
        this.sendSuccessful = sendSuccessful;
    }

    public int getLocalLamportCounter() {
        return localLamportCounter;
    }

    public String getErrrorMessage() {
        return errrorMessage;
    }

    public void setErrrorMessage(String errrorMessage) {
        this.errrorMessage = errrorMessage;
    }
}
