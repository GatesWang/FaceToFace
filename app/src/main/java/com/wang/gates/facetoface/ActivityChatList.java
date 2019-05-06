package com.wang.gates.facetoface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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


public class ActivityChatList extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final int CREATE_ACCOUNT = 1;
    private ArrayAdapter<Chat> chatsAdapter;
    private ArrayList<Chat> chatsArrayList;
    private ListView chatsListView;

    private User person;
    private Button newChatButton;

    private int chatSelectedPosition = -1;
    private AlertCreateChat alertDialog;
    private ChatList chatList;

    private void newChatBehavior(){
        newChatButton = findViewById(R.id.new_chat);
        newChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog = new AlertCreateChat(ActivityChatList.this, chatList);
                alertDialog.getBuilder().show();
            }
        });
    }
    private void setOnClick(){
        chatsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Chat chat = chatsAdapter.getItem(position);
                goToChat(person, chat);
            }
        });

        chatsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                chatSelectedPosition = position;
                openContextMenu(chatsListView);
                return true;
            }
        });
    }
    private void getUserInfo(){
        person = (User) getIntent().getSerializableExtra("person");
        addUserToDatabase(person);
        updateChatList();
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
    private void goToChat(User person, Chat chat){
        Intent intent = new Intent(ActivityChatList.this, ActivityChat.class);
        intent.putExtra("person", person);
        intent.putExtra("chat", chat);
        Bundle chatList = new Bundle();
        chatList.putSerializable("chatList", chatsArrayList);
        intent.putExtras(chatList);
        startActivity(intent);
    }
    private void goToSettings(MenuItem item){
        Intent i = new Intent(ActivityChatList.this, ActivityChatSettings.class);
        //chatSelectedPosition is set in setOnClick()
        Chat chatSelected = chatsArrayList.get(chatSelectedPosition);
        i.putExtra("chat", chatSelected);
        Bundle bundle = new Bundle();
        bundle.putSerializable("chatsArrayList", chatsArrayList);
        i.putExtras(bundle);
        startActivity(i);
    }
    private void goToEventCalendar(MenuItem item){
        Intent i = new Intent(ActivityChatList.this, ActivityEventCalendar.class);
        i.putExtra("user", person);
        if(chatSelectedPosition!=-1){
            //a particular chat is selected
            Chat chatSelected = chatsArrayList.get(chatSelectedPosition);
            i.putExtra("chat", chatSelected);
        }
        startActivity(i);
    }
    private void goToChatNotification(ChatList chatList){
        boolean gotochat = getIntent().getBooleanExtra("gotochat", false);
        if(gotochat){
            String chatKey = getIntent().getStringExtra("chatKey");
            chatList.goToChatNotification(this, chatKey, person);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        chatsListView = (ListView) findViewById(R.id.list_of_chats);
        registerForContextMenu(chatsListView);

        getUserInfo();
        goToChatNotification(chatList);
        setOnClick();
        newChatBehavior();

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("id",person.getId());
        editor.commit();
        startService();
    }

    private void startService(){
        startService(new Intent(this, ServiceNotification.class));
    }
    private void updateChatList(){
        chatsArrayList = new ArrayList<>();
        chatsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatsArrayList);
        chatsListView.setAdapter(chatsAdapter);
        chatList = new ChatList(ActivityChatList.this, chatsArrayList, chatsAdapter, chatsListView);
        chatList.displayChatList();
    }
    @Override
    protected void onStart() {
        super.onStart();
        chatSelectedPosition = -1;
        updateChatList();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        boolean chatDeleted = pref.getBoolean("chatDeleted",false);
        if(chatDeleted){
            chatList.displayChatList();
            editor.putBoolean("chatDeleted",false);
            editor.commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("choose what to do");
        menu.add(0, 0, 0, "settings");
        menu.add(1,1,1,"calendar");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        switch (item.getItemId()){
            case 0:
                goToSettings(item);
                break;
            case 1:
                goToEventCalendar(item);
                break;
        }
        return true;
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
                goToEventCalendar(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
