package com.example.gates.facetoface;


import java.io.Serializable;
import java.util.ArrayList;

public class Chat implements Serializable {
    private String chatName;
    private String chatKey;
    private ArrayList<String> memberIds;

    public Chat(){

    }

    public Chat(String chatName, String chatKey, ArrayList<String> memberIds) {
        this.chatName = chatName;
        this.chatKey = chatKey;
        this.memberIds = memberIds;
    }

    public String getChatName(){
        return chatName;
    }

    public String getChatKey(){
        return chatKey;
    }

    public ArrayList<String> getMemberIds(){
        return memberIds;
    }

    @Override
    public String toString() {
        return getChatName();
    }
}
