package com.example.gates.facetoface;

import java.util.Date;

public class ChatMessage {

    private String messageText;
    private String messageUser;
    private String messageImage;
    private long messageTime;

    public ChatMessage(String messageText, String messageUser) {
        this.messageText = messageText;
        this.messageUser = messageUser;

        // Initialize to current time
        this.messageTime = new Date().getTime();
    }

    public ChatMessage(String messageText, String messageUser, long messageTime) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.messageTime = messageTime;

        // Initialize to current time
        this.messageTime = new Date().getTime();
    }

    public ChatMessage(){

    }
    public void setImage(String messageImage){
        this.messageImage = messageImage;
    }
    public String getImage(){
        return messageImage;
    }
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
        return "message " + messageText + " user " + messageUser + " time " + messageTime  + " image " + messageImage ;
    }
}