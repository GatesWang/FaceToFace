package com.wang.gates.facetoface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ChatList {
    private ArrayList<Chat> chatsArrayList;
    private ArrayAdapter<Chat> chatsAdapter;
    private ListView chatsListView;
    private Context context;
    private String id;

    public ChatList(Context context, ArrayList<Chat> chatsArrayList, ArrayAdapter<Chat> chatsAdapter, ListView chatsListView){
        this.context = context;
        this.chatsArrayList = chatsArrayList;
        this.chatsAdapter = chatsAdapter;
        this.chatsListView = chatsListView;
        getInfo();
    }
    private void getInfo(){
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences("MyPref",0);
        id = preferences.getString("id",null);
    }
    public void displayChatList(){
        chatsArrayList.clear();
        chatsAdapter.clear();
        //only get chats of the current user
        DatabaseReference databaseMembers = FirebaseDatabase.getInstance().getReference("members");
        databaseMembers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chat : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {};
                    ArrayList<String> members = chat.child("memberIds").getValue(t);
                    if(members.contains(id)) {
                        chatsArrayList.add(chat.getValue(Chat.class));
                    }
                }
                chatsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, chatsArrayList);
                chatsListView.setAdapter(chatsAdapter);

            }
            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    public void goToChatNotification(final Activity activity, final String chatKey, final User person){
        final ArrayList<Chat> chatsArrayList2 = new ArrayList<>();
        DatabaseReference databaseMembers = FirebaseDatabase.getInstance().getReference("members");
        databaseMembers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chat : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {};
                    ArrayList<String> members = chat.child("memberIds").getValue(t);
                    if(members.contains(id)) {
                        chatsArrayList2.add(chat.getValue(Chat.class));
                    }
                }

                for(Chat chat: chatsArrayList2){
                    if(chat.getChatKey().equals(chatKey)){
                        Intent intent = new Intent(activity, ActivityChat.class);
                        intent.putExtra("person", person);
                        intent.putExtra("chat", chat);
                        Bundle chatList = new Bundle();
                        chatList.putSerializable("chatList", chatsArrayList);
                        intent.putExtras(chatList);
                        activity.startActivity(intent);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
}
