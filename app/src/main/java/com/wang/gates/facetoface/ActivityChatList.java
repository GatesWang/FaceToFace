package com.wang.gates.facetoface;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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


public class ActivityChatList extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final int CREATE_ACCOUNT = 1;
    private static AdapterTitleContent chatsAdapter;
    public static ArrayList<Chat> chatsArrayList;
    private static RecyclerView chatsRecyclerView;

    private static Activity activity;
    private static User person;
    private Button newChatButton;

    private AlertCreateChat alertDialog;

    private void newChatBehavior(){
        newChatButton = findViewById(R.id.new_chat);
        newChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog = new AlertCreateChat(ActivityChatList.this);
                alertDialog.getBuilder().show();
            }
        });
    }
    private void getUserInfo(){
        person = (User) getIntent().getSerializableExtra("person");
        addUserToDatabase(person);
    }
    private void signOut() {
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(ActivityChatList.this,
                                "You have been signed out.",
                                Toast.LENGTH_LONG)
                                .show();

                        // Close activity
                        Intent login = new Intent(ActivityChatList.this, ActivitySignIn.class);
                        login.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(login);
                        finish();
                    }
                });
    }
    private void addUserToDatabase(final User person){
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean registered = false;
                for(DataSnapshot data: dataSnapshot.getChildren()){
                    if (data.getKey().equals(person.getId())) {
                        registered = true;
                        person.setName(data.child("name").getValue().toString());
                        getSupportActionBar().setTitle("Welcome " + person.getName());  // provide compatibility to all the versions
                    }
                }
                if(!registered){
                    Intent registerIntent = new Intent(ActivityChatList.this, ActivityRegister.class);
                    registerIntent.putExtra("person", person);
                    startActivityForResult(registerIntent, CREATE_ACCOUNT);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void goToEventCalendar(){
        Intent i = new Intent(ActivityChatList.this, ActivityEventCalendar.class);
        i.putExtra("user", person);
        startActivity(i);
    }
    private void goToChatNotification(){
        boolean gotochat = getIntent().getBooleanExtra("gotochat", false);
        if(gotochat){
            final String chatKey = getIntent().getStringExtra("chatKey");
            DatabaseReference databaseMembers = FirebaseDatabase.getInstance().getReference("members");
            databaseMembers.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //get all the chats user is in
                    for (DataSnapshot chatJson : dataSnapshot.getChildren()) {
                        if (chatJson.getKey().equals(chatKey)) {
                            Intent intent = new Intent(ActivityChatList.this, ActivityChat.class);
                            intent.putExtra("person", person);
                            intent.putExtra("chat", chatJson.getValue(Chat.class));
                            Bundle chatList = new Bundle();
                            chatList.putSerializable("chatList", chatsArrayList);
                            intent.putExtras(chatList);
                            startActivity(intent);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {

                }
            });
        }
    }
    public static void getChats(){
        chatsArrayList = new ArrayList<>();
        //only get chats of the current user
        DatabaseReference databaseMembers = FirebaseDatabase.getInstance().getReference("members");
        databaseMembers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatsArrayList.clear();
                for (DataSnapshot chat : dataSnapshot.getChildren()) {
                    GenericTypeIndicator<ArrayList<String>> t = new GenericTypeIndicator<ArrayList<String>>() {};
                    ArrayList<String> members = chat.child("memberIds").getValue(t);
                    if(members.contains(person.getId())) {
                        chatsArrayList.add(chat.getValue(Chat.class));
                    }
                }
                updateRecyclerView();
            }
            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
    private static void updateRecyclerView(){
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        chatsRecyclerView.setLayoutManager(layoutManager);
        chatsAdapter = new AdapterTitleContent(chatsArrayList, activity);
        chatsRecyclerView.setAdapter(chatsAdapter);
    }
    public void startService(){
        startService(new Intent(this, ServiceNotification.class));
    }
    public User getPerson(){
        return person;
    }
    public ArrayList<Chat> getChatList(){
        return chatsArrayList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        chatsArrayList = new ArrayList<>();
        chatsRecyclerView = findViewById(R.id.list_of_chats);
        activity = this;

        getUserInfo();
        goToChatNotification();
        newChatBehavior();

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("id",person.getId());
        editor.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getChats();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        boolean chatDeleted = pref.getBoolean("chatDeleted",false);
        if(chatDeleted){
            editor.putBoolean("chatDeleted",false);
            editor.commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        activity = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==CREATE_ACCOUNT){
            person = (User) data.getSerializableExtra("person");
            getSupportActionBar().setTitle("Welcome " + person.getName());  // provide compatibility to all the versions
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                signOut();
                return true;
            case R.id.event_calendar:
                goToEventCalendar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
