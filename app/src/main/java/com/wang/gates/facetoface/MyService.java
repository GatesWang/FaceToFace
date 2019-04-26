package com.wang.gates.facetoface;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MyService extends Service {
    private final static String CHANNEL_ID = "meetup";
    private int chanelId = 0;
    private String id;
    private HashMap<String, String> chatKeys;
    //<id, name>
    private HashMap<String, String> chatJsonKeys;
    //<jsonkey, key>
    private HashMap<String,Boolean> chatNotifications;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this, "on create", Toast.LENGTH_SHORT).show();
        chatKeys = new HashMap<>();
        chatJsonKeys = new HashMap<>();
        //also adds listeners
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "On start", Toast.LENGTH_SHORT).show();
        getChatKeys();
        return Service.START_STICKY;
    }

    private void getChatKeys() {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        id = pref.getString("id",null);
        if(id!=null){
        //for each chat create a value event listener
            DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference().child("members");
            messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot chat: dataSnapshot.getChildren()){
                        GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>(){};
                        ArrayList<String> members = chat.child("memberIds").getValue(t);
                        for(String memberId: members){
                            if (memberId.equals(id)) {
                                chatKeys.put(chat.child("chatKey").getValue().toString(), chat.child("chatName").getValue().toString());
                            }
                        }
                    }
                    getChatJsonKeys(chatKeys);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
    private void getChatJsonKeys(final HashMap<String, String> chatKeys){
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference().child("messages");
        //get references to chats
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot chat: dataSnapshot.getChildren()){
                    String chatKey = chat.child("chatId").getValue().toString();
                    if(new ArrayList(chatKeys.keySet()).contains(chatKey)){
                        chatJsonKeys.put(chat.getKey(), chatKey);
                    }
                }
                //set listeners
                setListeners();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setListeners(){
        for(String chatJsonKey: chatJsonKeys.keySet()){
            final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("messages").child(chatJsonKey);
            chatRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    getLastMessage(chatRef);
                }
                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
    }

    //pass in chatRef, get last message
    private void getLastMessage(final DatabaseReference chatRef){
        Query lastQuery = chatRef.child("messages").orderByKey().limitToLast(1);
        lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<HashMap<String, ChatMessage>> t = new GenericTypeIndicator<HashMap<String, ChatMessage>>(){};
                HashMap<String,ChatMessage> map = dataSnapshot.getValue(t);
                if(map.size()>0) {
                    ChatMessage lastMessage = map.get(new ArrayList<>(map.keySet()).get(0));
                    Log.d(">>>", "" + lastMessage);

                    if(!lastMessage.getUserID().equals(id)){
                        //message not sent by user
                        //create notification
                        String key = chatJsonKeys.get(chatRef.getKey());
                        String chatName = chatKeys.get(key);
                        showNotification(chatName, lastMessage.toString());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Handle possible errors.
            }
        });
    }
    private void showNotification(String textTitle, String textContent) {
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.add)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        notificationManager.notify(chanelId, builder.build());
        chanelId++;
        Toast.makeText(MyService.this, "new chat", Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel(){
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
