package com.wang.gates.facetoface;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static android.Manifest.permission.READ_CONTACTS;

public class AlertCreateChat {
    private AlertDialog.Builder builder;
    private AdapterNewChatMember adapterNewChatMember;
    private ArrayAdapter<String> potentialMemberAdapter;
    private ArrayList<String> members;

    private Context context;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS  = 100;

    private EditText chatNameEditText;
    private AutoCompleteTextView newMemberInput;
    private ListView membersListView;
    private String id;
    private ChatList chatList;

    public AlertCreateChat(final Context context, ChatList chatList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ((Activity) context).requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        this.context = context;
        this.chatList = chatList;
        builder = new AlertDialog.Builder(context);
        builder.setTitle("New Chat");

        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        chatNameEditText = new EditText(context);
        newMemberInput = new AutoCompleteTextView(context);
        membersListView = new ListView(context);

        chatNameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        chatNameEditText.setHint("Chat name");
        newMemberInput.setInputType(InputType.TYPE_CLASS_TEXT);
        newMemberInput.setHint("Name");

        getInfo();
        members = new ArrayList<>();
        adapterNewChatMember = new AdapterNewChatMember(members, context);
        membersListView.setAdapter(adapterNewChatMember);
        setItemClick();

        potentialMemberAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                getContactNames());
        newMemberInput.setAdapter(potentialMemberAdapter);

        layout.addView(chatNameEditText);
        layout.addView(newMemberInput);
        layout.addView(membersListView);
        setUpPositive();
        setUpNegative();
        builder.setView(layout);
    }
    private void setItemClick(){
        newMemberInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {
                final String newMemberString = newMemberInput.getText().toString();
                if (newMemberString.length() > 0) {
                    //checks to see if member is proper
                    String[] info = newMemberString.split("\\s+");
                    final String phoneNumber = info[1] + info[2].replaceAll("-", "");
                    DatabaseReference databaseUsers = FirebaseDatabase.getInstance().getReference().child("users");
                    databaseUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            boolean inDatabase = false;

                            for (DataSnapshot user : dataSnapshot.getChildren()) {
                                if (user.child("number").getValue().equals(phoneNumber)) {
                                    members.add(newMemberString);
                                    adapterNewChatMember.notifyDataSetChanged();
                                    newMemberInput.setText("");
                                    inDatabase = true;
                                }
                            }
                            if (!inDatabase) {
                                Toast.makeText(context, "This user is not registered for the app", Toast.LENGTH_LONG).show();
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
    }
    private void setUpPositive(){
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                final String chatNameNew = chatNameEditText.getText().toString().trim();
                if (members.size() < 1) {
                    Toast.makeText(context, "There needs to be at least one other member", Toast.LENGTH_SHORT).show();
                } else if (chatNameNew.length() < 1) {
                    Toast.makeText(context, "The chat must have a valid name", Toast.LENGTH_SHORT).show();
                } else {
                    //create the chat object
                    DatabaseReference databaseUsers = FirebaseDatabase.getInstance().getReference().child("users");
                    databaseUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            ArrayList<String> memberIds = new ArrayList<>();
                            memberIds.add(id);
                            for (int i = 0; i < members.size(); i++) {
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
                            String newChatKey = newChatReference.getKey();
                            Chat newChat = new Chat(chatNameNew, newChatKey, memberIds);
                            newChatReference.setValue(newChat);
                            //create messages object
                            Messages messages = new Messages(newChatKey, new HashMap<String, ChatMessage>());
                            FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("messages")
                                    .push()
                                    .setValue(messages);

                            SharedPreferences pref = context.getSharedPreferences("MyPref", 0);
                            SharedPreferences.Editor editor = pref.edit();
                            //default is to show notifications
                            editor.putBoolean(newChatKey + "notifications", true);
                            editor.commit();

                            chatList.displayChatList();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                dialog.dismiss();
            }
        });
    }
    private void setUpNegative(){
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
    }
    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(context, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else {
            ((Activity)context).requestPermissions(new String[]{READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
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
        ContentResolver cr = context.getContentResolver();
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
        names = new ArrayList<>(set);
        return names;
    }
    private void getInfo(){
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences("MyPref",0);
        id = preferences.getString("id", null);
    }
    public AlertDialog.Builder getBuilder(){
        return builder;
    }
}
