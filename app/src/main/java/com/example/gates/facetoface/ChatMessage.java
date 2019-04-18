package com.example.gates.facetoface;

import java.util.Date;

public class ChatMessage {

    private String messageText;
    private String messageUser;
    private String userID;
    private long messageTime;

    public ChatMessage(String messageText, String messageUser, String userID) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.userID = userID;

        // Initialize to current time
        this.messageTime = new Date().getTime();
    }

    public ChatMessage(String messageText, String messageUser, String userID, long time) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.userID = userID;

        // Initialize to current time
        this.messageTime = time;
    }


    public ChatMessage(){

    }
    public String getUserID(){return userID;}

    public String getMessageText() {
        return messageText;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public long getMessageTime() {
        return messageTime;
    }
    @Override
    public String toString() {
        return "message " + messageText + " user " + messageUser + " time " + messageTime ;
    }
}