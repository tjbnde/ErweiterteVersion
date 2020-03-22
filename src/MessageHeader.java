import java.util.Date;
import java.util.UUID;

public class MessageHeader {
    private UUID messageId;
    private Client sentFrom;
    private Client sentTo;
    private Date timeSend;
    private Date timeReceived;
    private Date expirationDate;
    private boolean redeliverMessage;

    public MessageHeader(Client sendFrom, Client sendTo) {
        messageId = UUID.randomUUID();
        this.sentFrom = sendFrom;
        this.sentTo = sendTo;
        long millis = System.currentTimeMillis();
        timeSend = new Date(millis);
        timeReceived = null;
        // sets time of expiration 10 seconds after sending
        expirationDate = new Date(millis + 10000);
        redeliverMessage = false;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public Client getSentFrom() {
        return sentFrom;
    }

    public Client getSentTo() {
        return sentTo;
    }

    public Date getTimeSend() {
        return timeSend;
    }

    public Date getTimeReceived() {
        return timeReceived;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public boolean isRedeliverMessage() {
        return redeliverMessage;
    }

    public void setTimeReceived(Date timeReceived) {
        this.timeReceived = timeReceived;
    }

    public void setRedeliverMessage(boolean redeliverMessage) {
        this.redeliverMessage = redeliverMessage;
    }
}
