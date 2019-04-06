package com.example.gates.facetoface;

import android.content.Intent;

import android.net.Uri;
import android.support.annotation.NonNull;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ActivityChat extends AppCompatActivity {

    private User person;
    private String chatName;
    private Uri uri;
    private AdapterMessage messagesAdapter;
    private ArrayList<ChatMessage> messagesArrayList;
    private ListView messagesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messagesArrayList = new ArrayList<ChatMessage>();
        messagesListView = (ListView) findViewById(R.id.list_of_messages);
        messagesAdapter = new AdapterMessage(messagesArrayList,ActivityChat.this);
        messagesListView.setAdapter(messagesAdapter);

        getUserInfo();
        displayChatMessages();

        FloatingActionButton fab =
                (FloatingActionButton)findViewById(R.id.new_message);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = (EditText)findViewById(R.id.message_input);

                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("messages")
                        .child(chatName)
                        .push()
                        .setValue(new ChatMessage(
                                input.getText().toString(),
                                person.getName()));

                Log.d(">>>", ""+person.getName());
                // Clear the input
                input.setText("");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
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

    private void displayChatMessages() {
        //only display last 20 messages
        messagesArrayList.clear();
        //only get chats of the current user
        final String id = person.getId();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("messages").child(chatName);
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnap : dataSnapshot.getChildren()) {
                    ArrayList<String> temp = new ArrayList<String>();
                    for(DataSnapshot d : childSnap.getChildren()){
                        temp.add(d.getValue().toString());
                    }
                    ChatMessage chatMessage = new ChatMessage(temp.get(0), temp.get(2), Long.parseLong(temp.get(1)));
                    messagesArrayList.add(chatMessage);
                    Log.d(">>>", "" + messagesArrayList);
                }
                messagesAdapter = new AdapterMessage(messagesArrayList,ActivityChat.this);
                messagesListView.setAdapter(messagesAdapter);
            }
            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
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
            case R.id.menu_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void goToEventCalendar(){

    }

    private void getUserInfo(){
        Intent intent = getIntent();
        person = (User) intent.getSerializableExtra("person");
        chatName = (String) intent.getStringExtra("chat");
    }

}
