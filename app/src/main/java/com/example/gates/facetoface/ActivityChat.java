package com.example.gates.facetoface;

import android.content.Intent;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class ActivityChat extends AppCompatActivity {

    private User person;
    private String chatName;
    private Uri uri;
    private FirebaseListAdapter<ChatMessage> adapter;
    private ListView listOfMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

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
                                FirebaseAuth.getInstance().getCurrentUser().getDisplayName())
                        );

                Log.d(">>>", ""+chatName);
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
        person = (User) intent.getSerializableExtra("bundle");
        chatName = (String) intent.getStringExtra("chat");
    }

}
