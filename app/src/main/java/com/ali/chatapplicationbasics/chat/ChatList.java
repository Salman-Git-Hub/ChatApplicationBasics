package com.ali.chatapplicationbasics.chat;

public class ChatList {

    private final String name;
    private final String message;
    private final String time;
    private final String sender;

    public ChatList(String name, String message, String time, String sender) {
        this.name = name;
        this.message = message;
        this.time = time;
        this.sender = sender;
    }


    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public String getSender() {
        return sender;
    }
}
