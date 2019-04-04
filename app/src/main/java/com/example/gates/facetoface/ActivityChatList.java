package com.example.gates.facetoface;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
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
import android.widget.TextView;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.ArrayList;
import java.util.HashSet;
import static android.Manifest.permission.READ_CONTACTS;


public class ActivityChatList extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS  = 100;
    private ArrayAdapter<Chat> chatsAdapter;
    private ArrayList<Chat> chatsArrayList = new ArrayList<Chat>();
    private ListView chatsListView;

    private User person;
    private String id;
    private String number;
    private Button newChatButton;
    private FirebaseUser user;

    private ArrayAdapter<String> potentialMemberAdapter;
    private ArrayList<String> members;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        chatsListView = (ListView) findViewById(R.id.list_of_chats);
        chatsAdapter = new ArrayAdapter<Chat>(this, android.R.layout.simple_list_item_1, chatsArrayList);
        chatsListView.setAdapter(chatsAdapter);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        }

        getUserInfo();

        chatsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Chat chat = chatsAdapter.getItem(position);
                goToChat(person, chat);
            }
        });

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
                final MemberAdapter memberAdapter = new MemberAdapter(members, getApplicationContext());
                membersListView.setAdapter(memberAdapter);


                newMemberInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {
                        String newMemberString = newMemberInput.getText().toString();
                        if(newMemberString.length()>0) {
                            members.add(newMemberString);
                            memberAdapter.notifyDataSetChanged();
                            newMemberInput.setText("");
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
                    public void onClick(DialogInterface dialog, int which) {
                        String chatName = chatNameEditText.getText().toString();
                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("members")
                                .child(chatName)
                                .push()
                                .setValue(person.getId());

                        FirebaseDatabase.getInstance()
                                .getReference()
                                .child("messages")
                                .child(chatName)
                                .setValue(true);

                        displayChats();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        }
    }

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
                    String name=cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
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

    @Override
    protected void onStart() {
        super.onStart();
        displayChats();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void displayChats(){
        chatsArrayList.clear();
        //only get chats of the current user
        final String uid = user.getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("members");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnap : dataSnapshot.getChildren()) {
                    for(DataSnapshot d : childSnap.getChildren()){
                        if(d.getValue().equals(uid)) {
                            Log.d(">>>", ""+ childSnap.getKey());
                            chatsArrayList.add(new Chat(childSnap.getKey()));
                        }
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

    private void goToChat(User person, Chat chat){
        Intent intent = new Intent(ActivityChatList.this, ActivityChat.class);
        intent.putExtra("person", person);
        intent.putExtra("chat", chat.getChatName());
        startActivity(intent);
    }

    private void getUserInfo(){
        person = (User) getIntent().getSerializableExtra("bundle");
        number = person.getNumber();
        id = person.getId();
        addUserToDatabase(person);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                signOut();
            case R.id.event_calendar:
                goToEventCalendar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public void goToEventCalendar(){
        Intent i = new Intent(ActivityChatList.this, ActivityEventCalendar.class);
        startActivity(i);
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
                    if (data.child(person.getId()).exists()) {
                        registered = true;
                    }
                }
                if(!registered){
                    registerUser(person);
                }
            }

            @Override
            public void onCancelled(DatabaseError direbaseError) {

            }
        });
    }

    private void registerUser(User person){
        createDialogs();
    }
    private void createDialogs(){
        final AlertDialog.Builder builder1 = new AlertDialog.Builder(ActivityChatList.this);
        LinearLayout layout = new LinearLayout(ActivityChatList.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText nameLabel = new EditText(ActivityChatList.this);
        layout.addView(nameLabel);
        builder1.setView(layout);
        builder1.setCancelable(false);
        builder1.setTitle("Enter your name");

        final AlertDialog.Builder builder2 = new AlertDialog.Builder(ActivityChatList.this);
        LinearLayout layout2 = new LinearLayout(ActivityChatList.this);
        layout2.setOrientation(LinearLayout.VERTICAL);
        builder2.setView(layout2);
        builder2.setCancelable(false);
        builder2.setTitle("Need to enter name to continue");
        builder2.setPositiveButton("Try again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                createDialogs();
            }
        });

        builder2.setNegativeButton("Close app", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        final AlertDialog alert2 = builder2.create();

        builder1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //set name String
                person.setName(nameLabel.getText().toString());
                dialog.dismiss();
            }
        });

        builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alert2.show();
            }
        });

        builder1.show();
    }

}
