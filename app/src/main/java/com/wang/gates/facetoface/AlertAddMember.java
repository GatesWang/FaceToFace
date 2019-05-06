package com.wang.gates.facetoface;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AlertAddMember extends AlertCreateChat{
    private Chat chat;
    private ActivityChatSettings activityChatSettings;

    public AlertAddMember(final Context context, Chat chat, ActivityChatSettings activityChatSettings){
        super();
        this.context = context;
        this.chat = chat;
        this.activityChatSettings = activityChatSettings;

        builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Member");
        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        newMemberInput = new AutoCompleteTextView(context);
        membersListView = new ListView(context);
        newMemberInput.setInputType(InputType.TYPE_CLASS_TEXT);
        newMemberInput.setHint("Name");
        potentialMemberAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                getContactNames());
        newMemberInput.setAdapter(potentialMemberAdapter);

        getInfo();
        setItemClick();
        layout.addView(newMemberInput);
        layout.addView(membersListView);
        setUpPositive();
        setUpNegative();
        builder.setView(layout);
    }

    @Override
    protected void setUpPositive(){
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                DatabaseReference databaseUsers = FirebaseDatabase.getInstance().getReference().child("users");
                databaseUsers.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            ArrayList<String> memberIds = new ArrayList<>();
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
                        //make sure no duplicate members
                        boolean duplicates = false;
                        ArrayList oldMembers = chat.getMemberIds();
                        for(int i=0; i<oldMembers.size(); i++){
                            if(memberIds.contains(oldMembers.get(i))){
                                duplicates = true;
                            }
                        }
                        if(duplicates){
                            Toast.makeText(context, "There was a duplicate user", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                        else{
                            chat.getMemberIds().addAll(memberIds);
                            DatabaseReference chatRef = FirebaseDatabase.getInstance()
                                    .getReference()
                                    .child("members")
                                    .child(chat.getChatKey());

                            chatRef.setValue(chat);
                            activityChatSettings.getMembers();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        dialog.dismiss();
                    }
                });
            }
        });
    }

}
