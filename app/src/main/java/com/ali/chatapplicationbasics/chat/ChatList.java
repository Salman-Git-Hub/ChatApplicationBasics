package com.ali.chatapplicationbasics.chat;

public class ChatList {

    private String name, message, time, sender;

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
