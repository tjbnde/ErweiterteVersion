package Model;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable, Comparable {
    private MessageHeader header;
    private String text;
    private String status;

    public Message(String messageID, String sendFrom, String sendTo, String globalLamportCounterString, String sendSuccessfulString, String timeSendString, String text) {
        long timeSendLong = Long.parseLong(timeSendString);
        Date timeSend = new Date(timeSendLong);
        boolean sendSuccessful = Boolean.parseBoolean(sendSuccessfulString);
        int globalLamportCounter = Integer.parseInt(globalLamportCounterString);
        header = new MessageHeader(messageID, sendFrom, sendTo, globalLamportCounter, sendSuccessful, timeSend);
        this.text = text;
        status = "OK";
    }

    public Message(String sendFrom, String sendTo, int globalLamportCounter, String text) {
        header = new MessageHeader(sendFrom, sendTo, globalLamportCounter);
        this.text = text;
        status = "";
    }

    public String toString(){
        String messageToString = "";
        messageToString += header.getMessageId() + "#%#";
        messageToString += header.getSendFrom() + "#%#";
        messageToString += header.getSendTo() + "#%#";
        messageToString += header.getLocalLamportCounter() + "#%#";
        messageToString += header.isSendSuccessful() + "#%#";
        messageToString += header.getTimeSend().getTime() + "#%#";
        messageToString += text;
        return messageToString;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public MessageHeader getHeader() {
        return header;
    }

    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int compareTo(Object vMessage) {
        return this.getHeader().getLocalLamportCounter() - ((Message) vMessage).getHeader().getLocalLamportCounter();
    }
}
