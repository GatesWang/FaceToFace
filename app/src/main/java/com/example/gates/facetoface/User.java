package com.example.gates.facetoface;

import com.google.firebase.auth.FirebaseUser;
import java.io.Serializable;

public class User implements Serializable {
    private String number;
    private String id;
    private String name;
    private String imageB64;

    public User()
    {

    }

    public User(FirebaseUser user)
    {
        this.number = user.getPhoneNumber();
        this.id = user.getUid();
    }

    public String getNumber(){
        return number; }
    public String getId() {
        return id;}
    public String getName(){
        return name;
    }
    public String getImageB64(){
        return imageB64;
    }

    public void setName(String name){
        this.name = name;
    }
    public void setImageB64(String imageB64){
        this.imageB64 = imageB64;
    }
    public void setNumber(String number){
        this.number = number;
    }

    @Override
    public String toString() {
        return "number" + getNumber() + " name " + getName() + " image " + getImageB64().substring(0,10) + " id " + getId();
    }
}
