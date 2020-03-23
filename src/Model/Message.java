package Model;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {
    private MessageHeader header;
    private String text;

    public Message(String messageID, String sendFrom, String sendTo, String globalLamportCounterString, String sendSuccessfulString, String timeSendString, String text) {
        long timeSendLong = Long.parseLong(timeSendString);
        Date timeSend = new Date(timeSendLong);
        boolean sendSuccessful = Boolean.parseBoolean(sendSuccessfulString);
        int globalLamportCounter = Integer.parseInt(globalLamportCounterString);
        header = new MessageHeader(messageID, sendFrom, sendTo, globalLamportCounter, sendSuccessful, timeSend);
        this.text = text;
    }

    public Message(String sendFrom, String sendTo, int globalLamportCounter, String text) {
        header = new MessageHeader(sendFrom, sendTo, globalLamportCounter);
        this.text = text;
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
}
