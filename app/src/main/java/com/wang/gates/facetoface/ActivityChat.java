package com.wang.gates.facetoface;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

public class ActivityChat extends AppCompatActivity {

    private User person;
    private Chat chat;


    private AdapterMessage messagesAdapter;
    private ArrayList<ChatMessage> messagesArrayList = new ArrayList<ChatMessage>();
    private ListView messagesListView;

    private ArrayList<Chat> chatsArrayList = new ArrayList<>();
    private static final int CHAT_CHANGED = 10;

    private void signOut() {
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(ActivityChat.this,
                                "You have been signed out.",
                                Toast.LENGTH_LONG)
                                .show();

                        // Close activity
                        Intent login = new Intent(ActivityChat.this, ActivitySignIn.class);
                        login.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(login);
                        finish();
                    }
                });

    }

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
                            ArrayList<ChatMessage> messages = null;
                            String chatMessagesKey = "";
                            for(DataSnapshot chatJson: dataSnapshot.getChildren()){

                                if(chatJson.child("chatId").getValue().toString().equals(chat.getChatKey())){
                                    chatMessagesKey = chatJson.getKey();
                                    //get the messages
                                    GenericTypeIndicator<ArrayList<ChatMessage>> t = new GenericTypeIndicator<ArrayList<ChatMessage>>() {};
                                    messages = (ArrayList<ChatMessage>) chatJson.child("messages").getValue(t);
                                    //add messages
                                    //if no prior messages
                                    if(messages==null){
                                        messages = new ArrayList<>();
                                    }
                                    messages.add(chatMessage);
                                }
                            }
                            //send back to firebase
                            //set the arraylist as its new value
                            messagesReference.child(chatMessagesKey).child("messages").setValue(messages);
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
        messagesArrayList = new ArrayList<>();
        //get messages of this chat
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("messages");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot chatJson : dataSnapshot.getChildren()) {
                    if(chatJson.child("chatId").getValue().toString().equals(chat.getChatKey())){//if this is the chat that we want
                        //get messages
                        GenericTypeIndicator<ArrayList<ChatMessage>> t = new GenericTypeIndicator<ArrayList<ChatMessage>>() {};
                        messagesArrayList = chatJson.child("messages").getValue(t);
                    }
                }
                messagesAdapter = new AdapterMessage(messagesArrayList, ActivityChat.this);//update messages arraylist
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
        startActivityForResult(i, CHAT_CHANGED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messagesArrayList = new ArrayList<ChatMessage>();
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
