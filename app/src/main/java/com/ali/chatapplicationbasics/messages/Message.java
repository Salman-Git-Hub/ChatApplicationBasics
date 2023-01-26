package com.ali.chatapplicationbasics.messages;

import java.util.List;

public class Message {

    private String sender;
    private String message;
    private List<String> seenList;
    private String messageId;

    private String name;

    public Message() {
    }

    public Message(String sender, String name, String message, List<String> seenList) {
        this.sender = sender;
        this.name = name;
        this.message = message;
        this.seenList = seenList;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSender() {
        return sender;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getSeenList() {
        return seenList;
    }

    public String getMessageId() {
        return messageId;
    }
}
