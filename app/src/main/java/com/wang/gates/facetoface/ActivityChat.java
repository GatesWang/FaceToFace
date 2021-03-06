package com.wang.gates.facetoface;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ActivityChat extends AppCompatActivity {

    private User person;
    private Chat chat;


    private AdapterMessage messagesAdapter;
    private HashMap<String, ChatMessage> messagesHashMap = new HashMap<>();
    private ListView messagesListView;

    private ArrayList<Chat> chatsArrayList = new ArrayList<>();
    private static final int CHAT_CHANGED = 10;

    private void newChatMessage(){
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.new_message);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = (EditText)findViewById(R.id.message_input);
                String inputText = input.getText().toString();

                if(inputText.trim().equals("")){
                    Toast.makeText(ActivityChat.this, "Input must contain text", Toast.LENGTH_SHORT).show();
                }
                else{
                    final ChatMessage chatMessage = new ChatMessage(inputText, person.getName(), person.getId());
                    final DatabaseReference messagesReference = FirebaseDatabase.getInstance().getReference().child("messages");
                    messagesReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            HashMap<String, ChatMessage> messages = null;
                            String chatMessagesKey = "";
                            for(DataSnapshot chatJson: dataSnapshot.getChildren()){
                                if(chatJson.child("chatId").getValue().toString().equals(chat.getChatKey())){
                                    String messagesId = chatJson.child("messageId").getValue().toString();
                                    int id = Integer.parseInt(messagesId.substring(2));
                                    id++;
                                    chatMessagesKey = chatJson.getKey();
                                    //get the messages
                                    GenericTypeIndicator<HashMap<String, ChatMessage>> t = new GenericTypeIndicator<HashMap<String, ChatMessage>>(){};
                                    messages = chatJson.child("messages").getValue(t);
                                    //add messages
                                    //if no prior messages
                                    if(messages==null){
                                        messages = new HashMap<String, ChatMessage>();
                                    }
                                    messages.put("ID"+id, chatMessage);
                                    //send back to firebase
                                    messagesReference.child(chatMessagesKey).child("messages").setValue(messages);
                                    messagesReference.child(chatMessagesKey).child("messageId").setValue("ID"+id);
                                }
                            }
                            // Clear the input
                            input.setText("");
                            displayChatMessages();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
        });
    }

    private void displayChatMessages() {
        messagesHashMap = new HashMap<String, ChatMessage>();
        //get messages of this chat
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("messages");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatJson : dataSnapshot.getChildren()) {
                    if(chatJson.child("chatId").getValue().toString().equals(chat.getChatKey())){//if this is the chat that we want
                        //get messages
                        GenericTypeIndicator<HashMap<String, ChatMessage>> t = new GenericTypeIndicator<HashMap<String, ChatMessage>>() {};
                        messagesHashMap = chatJson.child("messages").getValue(t);
                    }
                }
                messagesAdapter = new AdapterMessage(messagesHashMap, ActivityChat.this);//update messages
                messagesListView.setAdapter(messagesAdapter);
            }
            @Override
            public void onCancelled(DatabaseError error) {

            }

        });

    }

    private void getUserInfo(){
        Intent intent = getIntent();
        person = (User) intent.getSerializableExtra("person");
        chat = (Chat) intent.getSerializableExtra("chat");
        chatsArrayList = (ArrayList<Chat>) intent.getExtras().get("chatList");
    }

    private void goToEventCalendar(){
        Intent i = new Intent(ActivityChat.this, ActivityEventCalendar.class);
        //a particular chat is selected
        i.putExtra("chat", chat);
        startActivity(i);
    }

    private void goToSettings(){
        Intent i = new Intent(ActivityChat.this, ActivityChatSettings.class);
        //chatSelectedPosition is set in setOnClick()
        i.putExtra("chat", chat);
        Bundle chatList = new Bundle();
        chatList.putSerializable("chatList", chatsArrayList);
        i.putExtras(chatList);
        i.putExtra("fromChat",true);
        startActivityForResult(i, CHAT_CHANGED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messagesHashMap = new HashMap<String, ChatMessage>();
        messagesListView = (ListView) findViewById(R.id.list_of_messages);
        messagesListView.setAdapter(messagesAdapter);

        getUserInfo();
        getSupportActionBar().setTitle(chat.getChatName());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        displayChatMessages();
        newChatMessage();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        boolean chatDeleted = pref.getBoolean("chatDeleted",false);
        if(chatDeleted){
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.settings:
                goToSettings();
                return true;
            case R.id.calendar:
                goToEventCalendar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == CHAT_CHANGED){
            if(resultCode == Activity.RESULT_OK){
                String newName = (String) data.getStringExtra("chatnewname");
                getSupportActionBar().setTitle(newName);
            }
        }
    }
}
