package com.wang.gates.facetoface;


import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
