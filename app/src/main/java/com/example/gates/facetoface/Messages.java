package com.example.gates.facetoface;

import java.util.ArrayList;

public class Messages {
    private String chatId;
    private ArrayList<ChatMessage> messages;

    public Messages(){

    }

    public Messages(String chatId, ArrayList<ChatMessage> messages) {
        this.chatId = chatId;
        this.messages = messages;
    }

    public String getChatId(){
        return chatId;
    }
    public ArrayList<ChatMessage> getMessages(){
        return messages;
    }

    public void add(ChatMessage chatMessage){
        this.messages.add(chatMessage);
    }
    @Override
    public String toString() {
        return getChatId();
    }
}
