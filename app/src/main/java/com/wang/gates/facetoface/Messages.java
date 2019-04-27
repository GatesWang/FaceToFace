package com.wang.gates.facetoface;

import java.util.HashMap;

public class Messages {
    private String chatId;
    private HashMap<String, ChatMessage> messages;
    private String messageId;

    public Messages(){

    }

    public Messages(String chatId, HashMap<String, ChatMessage> messages) {
        this.chatId = chatId;
        this.messages = messages;
        this.messageId = "ID0";
    }

    public String getChatId(){
        return chatId;
    }
    public HashMap<String, ChatMessage> getMessages(){
        return messages;
    }

    public void add(ChatMessage chatMessage){
        int id = Integer.parseInt(messageId.substring(2));
        id++;
        this.messages.put(messageId, chatMessage);
        messageId = "ID"+Integer.toString(id);
    }

    public String getMessageId(){
        return messageId;
    }
    @Override
    public String toString() {
        return getChatId();
    }
}
