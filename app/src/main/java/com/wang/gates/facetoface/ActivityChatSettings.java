package com.wang.gates.facetoface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ActivityChatSettings extends Activity implements  View.OnClickListener{
    private Chat chat;

    private Button renameChat;
    private TextView nameLabel;
    private ArrayList<Chat> chatList;
    private HashMap<String, String> memberNumbers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_settings);

        getInfo();
        setUpNotifications();

        nameLabel = findViewById(R.id.name_label);
        nameLabel.setText(chat.getChatName());
        renameChat = findViewById(R.id.chat_rename);
        renameChat.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.chat_rename:
                renameChat();
                break;
        }
    }

    private void getInfo(){
        Intent i = getIntent();
        chat = (Chat) i.getSerializableExtra("chat");
        chatList =  (ArrayList<Chat>) i.getExtras().get("chatList");
    }

    private void renameChat(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(ActivityChatSettings.this);
        builder.setTitle("Rename chat");

        final LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText renameEditText = new EditText(ActivityChatSettings.this);

        renameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        renameEditText.setText(chat.getChatName());

        layout.addView(renameEditText);
        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                final String chatNameNew = renameEditText.getText().toString().trim();
                boolean chatExists = false;
                for(Chat chat: chatList){
                    if(chat.getChatName().equals(chatNameNew)){
                        chatExists = true;
                    }
                }
                if(chatExists){
                    Toast.makeText(ActivityChatSettings.this, "That chat already exists", Toast.LENGTH_SHORT).show();
                    renameEditText.setText("");
                }
                else{
                    final DatabaseReference membersRef = FirebaseDatabase.getInstance().getReference().child("members");
                    membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot chatJson: dataSnapshot.getChildren()){
                                if(chatJson.child("chatKey").getValue().toString().equals(chat.getChatKey())){//find the chat we want
                                    String chatJsonKey = chatJson.getKey();
                                    membersRef.child(chatJsonKey).child("chatName").setValue(chatNameNew);
                                    nameLabel.setText(chatNameNew);
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void setUpNotifications(){
        Spinner spinner = (Spinner) findViewById(R.id.notifications_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.notification_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void listMembers(){
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("members").child(chat.getChatKey());
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot chat: dataSnapshot.getChildren()){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
