package com.wang.gates.facetoface;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static android.Manifest.permission.READ_CONTACTS;


public class ActivityChatList extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS  = 100;
    private static final int CREATE_ACCOUNT = 1;
    private ArrayAdapter<Chat> chatsAdapter;
    private ArrayList<Chat> chatsArrayList = new ArrayList<>();
    private ListView chatsListView;

    private FirebaseUser user;
    private User person;
    private String id;
    private String number;
    private Button newChatButton;
    private String newChatKey;

    private int chatSelectedPosition = -1;

    private AdapterNewChatMember adapterNewChatMember;
    private ArrayAdapter<String> potentialMemberAdapter;
    private ArrayList<String> members;

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
         else {
            this.requestPermissions(new String[]{READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        return false;
    }
    private ArrayList<String> getContactNames(){
        if (!mayRequestContacts()) {
            Log.d("ccc", "no cant get contacts");
            return null;
        }
        Log.d("ccc", "getting contacts");

        ArrayList<String> names = null;
        HashSet<String> set = new HashSet<String>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor cur1 = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id}, null);
                while (cur1.moveToNext()) {
                    //to get the contact names
                    String name = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    try {
                        Phonenumber.PhoneNumber swissNumberProto = phoneUtil.parse(number, "US");
                        number = phoneUtil.format(swissNumberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                    } catch (NumberParseException e) {
                        System.err.println("NumberParseException was thrown: " + e.toString());
                    }
                    if(number!=null){
                        set.add(name + " " + number);
                    }
                }
                cur1.close();
            }
        }
        names = new ArrayList<String>(set);
        return names;
    }
    private void newChatBehavior(){
        newChatButton = findViewById(R.id.new_chat);
        newChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderChat = new AlertDialog.Builder(ActivityChatList.this);
                builderChat.setTitle("Create New Chat");

                final LinearLayout layout = new LinearLayout(getApplicationContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText chatNameEditText = new EditText(ActivityChatList.this);
                final AutoCompleteTextView newMemberInput = new AutoCompleteTextView(ActivityChatList.this);
                final ListView membersListView = new ListView(getApplicationContext());

                chatNameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                chatNameEditText.setHint("Chat name");
                newMemberInput.setInputType(InputType.TYPE_CLASS_TEXT);
                newMemberInput.setHint("Phone number");

                members = new ArrayList<>();
                adapterNewChatMember = new AdapterNewChatMember(members, getApplicationContext());
                membersListView.setAdapter(adapterNewChatMember);
                newMemberInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {
                        final String newMemberString = newMemberInput.getText().toString();
                        if(newMemberString.length()>0) {
                            //checks to see if member is proper
                            String[] info = newMemberString.split("\\s+");
                            final String phoneNumber = info[1] + info[2].replaceAll("-","");
                            DatabaseReference databaseUsers = FirebaseDatabase.getInstance().getReference().child("users");
                            databaseUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    boolean inDatabase = false;

                                    for(DataSnapshot user: dataSnapshot.getChildren()){
                                        if(user.child("number").getValue().equals(phoneNumber)){
                                            //cant self add
                                            if(phoneNumber.equals(person.getNumber())){
                                                Toast.makeText(ActivityChatList.this, "You cannot add yourself, you are automatically added", Toast.LENGTH_SHORT).show();
                                                inDatabase = true;
                                                newMemberInput.setText("");
                                                break;
                                            }
                                            //user is already in database, good
                                            members.add(newMemberString);
                                            adapterNewChatMember.notifyDataSetChanged();
                                            newMemberInput.setText("");
                                            inDatabase = true;
                                        }
                                    }
                                    if(!inDatabase){
                                        Toast.makeText(ActivityChatList.this,"This user is not registered for the app", Toast.LENGTH_LONG).show();
                                        newMemberInput.setText("");
                                    }

                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                });

                potentialMemberAdapter = new ArrayAdapter<String>(
                        ActivityChatList.this,
                        android.R.layout.simple_dropdown_item_1line,
                        getContactNames());
                newMemberInput.setAdapter(potentialMemberAdapter);

                layout.addView(chatNameEditText);
                layout.addView(newMemberInput);
                layout.addView(membersListView);
                builderChat.setView(layout);

                // Set up the buttons
                builderChat.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        final String chatNameNew = chatNameEditText.getText().toString().trim();
                        boolean chatExists = false;
                        for(Chat chat: chatsArrayList){
                            if(chat.getChatName().equals(chatNameNew)){
                                chatExists = true;
                            }
                        }
                        if(chatExists){
                            Toast.makeText(ActivityChatList.this, "That chat already exists", Toast.LENGTH_SHORT).show();
                            chatNameEditText.setText("");
                        }
                        else if(members.size()<1){
                            Toast.makeText(ActivityChatList.this, "There needs to be at least one other member", Toast.LENGTH_SHORT).show();
                        }
                        else if(chatNameNew.length()<1){
                            Toast.makeText(ActivityChatList.this, "The chat must have a valid name", Toast.LENGTH_SHORT).show();

                        }
                        else{
                            //create the chat object
                            DatabaseReference databaseUsers = FirebaseDatabase.getInstance().getReference().child("users");
                            databaseUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    ArrayList<String> memberIds = new ArrayList<>();
                                    memberIds.add(id);
                                    for(int i=0; i<members.size(); i++) {
                                        String member = members.get(i);
                                        String[] memberInfo = member.split("\\s+");
                                        final String newMemberPhoneNumber = memberInfo[1] + memberInfo[2].replaceAll("-", "");
                                        //compare to all users, get the ones you want, stored in memberIds
                                        for (DataSnapshot user : dataSnapshot.getChildren()) {
                                            if (user.child("number").getValue().toString().equals(newMemberPhoneNumber)) {
                                                String newChatMemberID = user.child("id").getValue().toString();
                                                memberIds.add(newChatMemberID);
                                            }
                                        }
                                    }
                                    DatabaseReference newChatReference = FirebaseDatabase.getInstance()
                                            .getReference()
                                            .child("members")
                                            .push();
                                    newChatKey = newChatReference.getKey();
                                    Chat newChat = new Chat(chatNameNew, newChatKey, memberIds);
                                    newChatReference.setValue(newChat);
                                    //create messages object
                                    Messages messages = new Messages(newChatKey, new HashMap<String, ChatMessage>());
                                    FirebaseDatabase.getInstance()
                                            .getReference()
                                            .child("messages")
                                            .push()
                                            .setValue(messages);

                                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
                                    SharedPreferences.Editor editor = pref.edit();
                                    //default is to show notifications
                                    editor.putBoolean(newChatKey + "notifications", true);
                                    editor.commit();
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                                });
                            }

                            dialog.dismiss();
                            displayChatList();
                        }
                });

                builderChat.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builderChat.show();
            }
        });

    }
    private void displayChatList(){
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
                        chatsArrayList.add((Chat) chat.getValue(Chat.class));
                    }
                }
                chatsAdapter = new ArrayAdapter<Chat>(getApplicationContext(), android.R.layout.simple_list_item_1, chatsArrayList);
                chatsListView.setAdapter(chatsAdapter);

            }
            @Override
            public void onCancelled(DatabaseError error) {

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
        number = person.getNumber();
        id = person.getId();
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
        Bundle chatList = new Bundle();
        chatList.putSerializable("chatList", chatsArrayList);
        i.putExtras(chatList);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        chatsListView = (ListView) findViewById(R.id.list_of_chats);
        registerForContextMenu(chatsListView);
        chatsAdapter = new ArrayAdapter<Chat>(this, android.R.layout.simple_list_item_1, chatsArrayList);
        chatsListView.setAdapter(chatsAdapter);

        user = FirebaseAuth.getInstance().getCurrentUser();

        //if sdk version is greater than M and we dont not have permission, then request for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        }

        getUserInfo();
        setOnClick();
        newChatBehavior();
        //start service
        startService();
    }

    private void startService(){
        startService(new Intent(this, MyService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        chatSelectedPosition = -1;
        displayChatList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //shared preferences
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("id",person.getId());
        editor.commit();
        startService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        }
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
