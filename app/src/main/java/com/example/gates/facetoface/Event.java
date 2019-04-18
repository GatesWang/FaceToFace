package com.example.gates.facetoface;

import java.io.Serializable;
import java.util.HashMap;

public class Event implements Serializable {
    private String eventKey;
    private Chat chat;
    private String eventName;
    private String date;
    private String time;
    private HashMap<String, Boolean> memberStatus;


    public Event(){

    }

    public Event(String eventName, String date, String time, HashMap<String, Boolean> memberStatus, Chat chat) {
        this.eventName = eventName;
        this.chat = chat;
        this.date = date;
        this.time = time;
        this.memberStatus = memberStatus;
    }

    public String getTime(){
        return time;
    }
    public String getDate(){
        return date;
    }
    public Chat getChat(){
        return chat;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey){
        this.eventKey = eventKey;
    }

    public HashMap<String, Boolean> getMemberStatus() {
        return memberStatus;
    }


    public String getEventName(){
        return eventName;
    }

    @Override
    public String toString() {
        return getDate();
    }
}
