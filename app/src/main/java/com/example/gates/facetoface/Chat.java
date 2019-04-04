package com.example.gates.facetoface;


public class Chat {
    private String chatName;

    public Chat(){

    }

    public Chat(String chatName) {
        this.chatName = chatName;
    }

    public String getChatName(){
        return chatName;
    }

    @Override
    public String toString() {
        return getChatName();
    }
}
