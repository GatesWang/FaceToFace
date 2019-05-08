package com.wang.gates.facetoface;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
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

public class ServiceNotification extends Service {
    private final static String CHANNEL_ID = "notification";
    private int notificationId = 0;
    private String id;
    private HashMap<String, String> chatKeys;
    //<id, name>
    private HashMap<String, String> chatJsonKeys;
    //<jsonkey, key>
    private HashMap<ChildEventListener, String> listeners;
    //<listener, listenerId>

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

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
        listeners = new HashMap<>();

        preferences = getApplicationContext().getSharedPreferences("MyPref",0);
        editor = preferences.edit();

    }
    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "On start", Toast.LENGTH_SHORT).show();
        getChatKeys();
        return START_STICKY;
    }
    private void getChatKeys() {
        id = preferences.getString("id",null);
        if(id!=null){
        //get all the chatKeys for each chat the user is in
            DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference().child("members");
            messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot chat: dataSnapshot.getChildren()){
                        GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>(){};
                        ArrayList<String> members = chat.child("memberIds").getValue(t);
                        for(String memberId: members){
                            if (memberId!=null && memberId.equals(id)) {
                                chatKeys.put(chat.child("chatKey").getValue().toString(), chat.child("chatName").getValue().toString());
                                //<key, name>
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
        //get references to chats (jsonKey)
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
        for(final String chatJsonKey: chatJsonKeys.keySet()){

            String key = chatJsonKeys.get(chatJsonKey);
            //test if listener already exists
            if(listeners.get(key)!=null){
                continue;
            }
            else{
                final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("messages").child(chatJsonKey);
                ChildEventListener listener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Log.d(">>>", "listenid " + id);
                        boolean notify = preferences.getBoolean(chatJsonKeys.get(chatJsonKey) + "notifications", true);
                        if (notify == true) {
                            String id = listeners.get(this);
                            editor.putBoolean("id"+notificationId, true);
                            getLastMessage(chatRef);
                        }
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                };
                listeners.put(listener, chatJsonKeys.get(chatJsonKey));
                chatRef.addChildEventListener(listener);
            }
        }
    }
    private void getLastMessage(final DatabaseReference chatRef){
        Query queryMessages = chatRef.child("messages").orderByChild("messageTime");;
        Query lastQuery = queryMessages.limitToLast(1);
        lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<HashMap<String, ChatMessage>> t = new GenericTypeIndicator<HashMap<String, ChatMessage>>(){};
                HashMap<String,ChatMessage> map = dataSnapshot.getValue(t);
                if(map.size()>0) {
                    ChatMessage lastMessage = map.get(new ArrayList<>(map.keySet()).get(0));
                    if(true || !lastMessage.getUserID().equals(id)){
                        //message not sent by user
                        //create notification
                        String key = chatJsonKeys.get(chatRef.getKey());
                        String chatName = chatKeys.get(key);
                        showNotification(chatName, lastMessage.getMessageUser() + " : "  + lastMessage.getMessageText(), key);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Handle possible errors.
            }
        });
    }
    private void showNotification(String textTitle, String textContent, String chatKey) {
        createNotificationChannel();

        // Create an Intent for the activity you want to start
        Intent loginIntent = new Intent(this, ActivitySignIn.class);
        //put in special bundle to let activity know to start another activity
        loginIntent.putExtra("gotochat",true);
        loginIntent.putExtra("chatKey",chatKey);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(loginIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.add)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(resultPendingIntent);
        builder.setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, builder.build());
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
