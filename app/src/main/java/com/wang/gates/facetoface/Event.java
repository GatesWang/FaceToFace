package com.wang.gates.facetoface;

import java.io.Serializable;
import java.util.HashMap;

public class Event implements Serializable {
    private String eventKey;
    private String chatKey;
    private String eventName;
    private String date;
    private String time;
    private HashMap<String, Boolean> memberStatus;


    public Event(){

    }

    public Event(String eventName, String date, String time, HashMap<String, Boolean> memberStatus, String chatKey) {
        this.eventName = eventName;
        this.chatKey = chatKey;
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
    public String getChatKey(){
        return chatKey;
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
        return getTime();
    }
}
