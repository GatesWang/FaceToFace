package com.wang.gates.facetoface;

import java.util.Date;

public class ChatMessage {

    private String messageText;
    private String messageUser;
    private String userID;
    private String messageTime;

    public ChatMessage(String messageText, String messageUser, String userID) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.userID = userID;

        // Initialize to current time
        this.messageTime = "" + new Date().getTime();
    }

    public ChatMessage(String messageText, String messageUser, String userID, long time) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.userID = userID;

        // Initialize to current time
        this.messageTime = "" + time;
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

    public String getMessageTime() {
        return messageTime;
    }

    @Override
    public String toString() {
        return messageUser + " : " + messageText;
    }
}