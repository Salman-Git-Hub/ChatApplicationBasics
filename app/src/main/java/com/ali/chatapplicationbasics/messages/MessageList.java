package com.ali.chatapplicationbasics.messages;

public class MessageList {

    private final String name;
    private final String groupId;
    private final String lastMessage;
    private final String profilePic;
    private String messageId;


    private final String lastSender;
    private final int unSeenMessages;

    public MessageList(String name, String groupId, String lastMessage, String profilePic, int unSeenMessages, String lastSender) {
        this.name = name;
        this.groupId = groupId;
        this.lastSender = lastSender;
        this.lastMessage = lastMessage;
        this.profilePic = profilePic;
        this.unSeenMessages = unSeenMessages;
    }

    public String getLastSender() {
        return lastSender;
    }

    public String getName() {
        return name;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public int getUnSeenMessages() {
        return unSeenMessages;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
