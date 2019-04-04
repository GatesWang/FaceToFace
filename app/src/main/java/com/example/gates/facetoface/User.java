package com.example.gates.facetoface;

import com.google.firebase.auth.FirebaseUser;
import java.io.Serializable;

public class User implements Serializable {
    private String number;
    private String id;
    private String name;

    public User()
    {

    }

    public User(FirebaseUser user)
    {
        this.number = user.getPhoneNumber();
        this.id = user.getUid();
    }

    public String getNumber(){ return number; }
    public String getId() { return id;}
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setNumber(String number){
        this.number = number;
    }
}
