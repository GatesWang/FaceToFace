package com.wang.gates.facetoface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ActivityChatSettings extends AppCompatActivity implements  View.OnClickListener{
    private Chat chat;

    private Button renameChat;

    private ArrayList<String> memberIds = new ArrayList<>();
    private HashMap<String, String> idToName = new HashMap<String, String>();

    private Button addMemberButton;
    private RecyclerView memberRecyclerView;
    private LinearLayoutManager layoutManager = new LinearLayoutManager(ActivityChatSettings.this);
    private HashMap<String, String> memberNumbers = new HashMap<>();
    private AdapterMember adapterMember;
    //stores <Name, Number>
    private AlertAddMember addMember;

    private ArrayList<Chat> chatsArrayList;
    private String newName;

    private AdapterNewChatMember adapterNewChatMember;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_settings);

        getInfo();
        adapterMember = new AdapterMember(memberNumbers, idToName, chat, ActivityChatSettings.this);
        setUpNotifications();

        renameChat = findViewById(R.id.chat_rename);
        memberRecyclerView = findViewById(R.id.members_list);
        addMemberButton = findViewById(R.id.add_member);

        renameChat.setOnClickListener(this);
        addMemberButton.setOnClickListener(this);
        getMembers();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(chat.getChatName() + " settings");
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.chat_rename:
                renameChat();
                break;
            case R.id.add_member:
                addMember();
                break;
        }
    }

    private void getInfo(){
        Intent i = getIntent();
        chat = (Chat) i.getSerializableExtra("chat");
        chatsArrayList =  (ArrayList<Chat>) i.getExtras().get("chatsArrayList");
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
                final DatabaseReference membersRef = FirebaseDatabase.getInstance().getReference().child("members");
                membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot chatJson: dataSnapshot.getChildren()){
                            if(chatJson.child("chatKey").getValue().toString().equals(chat.getChatKey())){//find the chat we want
                                String chatJsonKey = chatJson.getKey();
                                membersRef.child(chatJsonKey).child("chatName").setValue(chatNameNew);
                                newName = chatNameNew;
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("chatnewname", newName);
                                getSupportActionBar().setTitle(newName + " settings");
                                setResult(Activity.RESULT_OK, returnIntent);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
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
        final String notifyPreferenceKey = chat.getChatKey() + "notifications";
        final SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        final SharedPreferences.Editor editor = pref.edit();
        boolean notify = pref.getBoolean(notifyPreferenceKey, true);
        if(notify){
            spinner.setSelection(getIndex(spinner, "On"));
        }
        else{
            spinner.setSelection(getIndex(spinner, "Off"));
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position==0){
                    //turn on
                    //default is to show notifications
                    editor.putBoolean(notifyPreferenceKey, true);
                    editor.commit();
                    //Log.d(">>>", "" + pref.getBoolean(chat.getChatKey() + "notifications",true));
                }
                else if(position == 1){
                    //turn off
                    //default is to show notifications
                    editor.putBoolean(notifyPreferenceKey, false);
                    editor.commit();
                    //Log.d(">>>", "" + pref.getBoolean(chat.getChatKey() + "notifications",true));
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) { }
        });
    }
    private int getIndex(Spinner spinner, String myString){
        int index = 0;
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).equals(myString)){
                index = i;
            }
        }
        return index;
    }
    public void getMembers(){
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("members").child(chat.getChatKey()).child("memberIds");
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot member: dataSnapshot.getChildren()){
                    memberIds.add(member.getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot user: dataSnapshot.getChildren()){
                    if(memberIds.contains(user.child("id").getValue().toString())){// if this user is a member
                        idToName.put(user.child("name").getValue().toString(), user.child("id").getValue().toString());
                        memberNumbers.put(user.child("name").getValue().toString(), user.child("number").getValue().toString());
                    }
                }
                updateRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void updateRecyclerView(){
        layoutManager = new LinearLayoutManager(this);
        memberRecyclerView.setLayoutManager(layoutManager);
        adapterMember = new AdapterMember(memberNumbers, idToName, chat, ActivityChatSettings.this);
        memberRecyclerView.setAdapter(adapterMember);
    }
    private void addMember(){
        addMember = new AlertAddMember(ActivityChatSettings.this, chat, this);
        addMember.getBuilder().show();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
