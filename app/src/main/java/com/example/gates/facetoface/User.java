package com.example.gates.facetoface;

import android.graphics.Bitmap;

import com.google.firebase.auth.FirebaseUser;
import java.io.Serializable;

public class User implements Serializable {
    private String number;
    private String id;
    private String name;
    private Bitmap image;

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
    public Bitmap getImage(){
        return image;
    }

    public void setName(String name){
        this.name = name;
    }
    public void setImage(Bitmap image){
        this.image = image;
    }
    public void setNumber(String number){
        this.number = number;
    }

    @Override
    public String toString() {
        return "number" + getNumber() + " name " + getName();
    }
}
