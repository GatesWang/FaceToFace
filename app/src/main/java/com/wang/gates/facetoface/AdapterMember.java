package com.wang.gates.facetoface;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

public class AdapterMember extends RecyclerView.Adapter<AdapterMember.ViewHolder> {
    private HashMap<String, String> memberNumbers;
    //<name, number>
    private HashMap<String, String> members;
    //<name, id>
    private AdapterMember adapter;
    private Chat chat;
    private Activity activity;
    private ChatList chatList;


    private static final int CALL_REQUEST = 15;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the view   s for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView memberName;
        public TextView memberNumber;
        public ImageView memberPicture;
        public Button removeButton;

        private final Context context;

        public View layout;

        public ViewHolder(View v) {
            super(v);
            context = v.getContext();
            layout = v;

            memberName = v.findViewById(R.id.member_name);
            memberNumber = v.findViewById(R.id.member_number);
            memberPicture = v.findViewById(R.id.member_picture);
            removeButton = v.findViewById(R.id.remove_button);
        }
    }

    public void remove(int position) {
        memberNumbers.remove(members.keySet().toArray()[position]);
        members.remove(members.keySet().toArray()[position]);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, memberNumbers.size());
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterMember(HashMap<String, String> memberNumbers, HashMap<String, String> members, Chat chat, ChatList chatList, Activity activity) {
        this.memberNumbers = memberNumbers;
        this.members = members;
        this.chat = chat;
        this.adapter = this;
        this.chatList = chatList;
        this.activity = activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdapterMember.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.member_row_layout, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    //when clicked
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        String name = (String) memberNumbers.keySet().toArray()[position];
        String number = memberNumbers.get(name);
        SpannableString numberLined = new SpannableString(number);
        numberLined.setSpan(new UnderlineSpan(), 0, numberLined.length(), 0);


        holder.memberName.setText(name);
        holder.memberNumber.setText(numberLined);

        final String id = members.get(name);
        StorageReference profileRef = FirebaseStorage.getInstance().getReference("profile_pictures");
        StorageReference userRef = profileRef.child(id);

        GlideApp.with(holder.context)
                .load(userRef)
                .into(holder.memberPicture);

        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeMember(id, position);
            }
        });

        holder.memberNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callPhoneNumber(holder.memberNumber.getText().toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberNumbers.size();
    }

    private void removeMember(final String id, final int position){
        //remove person from members
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference().child("members").child(chat.getChatKey()).child("memberIds");
        final DatabaseReference events =  FirebaseDatabase.getInstance().getReference().child("events");
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot member: dataSnapshot.getChildren()){
                    if(member.getValue().toString().equals(id)){
                        chatRef.child(member.getKey()).removeValue();
                        remove(position);
                        chat.getMemberIds().remove(id);
                        deleteChat(id);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //remove person from events
        events.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot event: dataSnapshot.getChildren()){
                    for(DataSnapshot member: event.child("memberStatus").getChildren()){
                        if(member.getKey().equals(id)){
                            member.getRef().setValue(null);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteChat(final String currentUserId){
        //tests to see if current chat has at least two members, then deletes chat
        DatabaseReference membersRef = FirebaseDatabase.getInstance().getReference().child("members").child(chat.getChatKey()).child("memberIds");
        final DatabaseReference members =  FirebaseDatabase.getInstance().getReference().child("members");
        final DatabaseReference messages =  FirebaseDatabase.getInstance().getReference().child("messages");
        final DatabaseReference events =  FirebaseDatabase.getInstance().getReference().child("events");

        membersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count = dataSnapshot.getChildrenCount();
                SharedPreferences pref = activity.getSharedPreferences("MyPref", 0);
                String spId = pref.getString("id",null);
                if(spId.equals(currentUserId) || count<2){
                    if(count<2){
                        Toast.makeText(activity, "Chats must have at least two people, this chat will be deleted", Toast.LENGTH_SHORT).show();
                        //deletes from members
                        messages.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot chatJson: dataSnapshot.getChildren()){
                                    if(chatJson.child("chatId").getValue().toString().equals(chat.getChatKey())){
                                        messages.child(chatJson.getKey()).removeValue();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                        //deletes from messages
                        members.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot chatJson: dataSnapshot.getChildren()){
                                    if(chatJson.child("chatKey").getValue().toString().equals(chat.getChatKey())){
                                        members.child(chatJson.getKey()).removeValue();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                        //deletes all events created by this chat
                        events.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot event: dataSnapshot.getChildren()){
                                    Log.d(">>>", chat.getChatKey());
                                    Log.d(">>>", event.child("chat").child("chatKey").getValue().toString());

                                    if(event.child("chat").child("chatKey").getValue().equals(chat.getChatKey())){
                                        event.getRef().setValue(null);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                        //restarts
                        restart();
                    }
                    else if(spId.equals(currentUserId)){
                        Toast.makeText(activity, "You removed yourself from this chat", Toast.LENGTH_SHORT).show();
                        restart();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void callPhoneNumber(String number) {
        try
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((ActivityChatSettings) activity, new String[]{Manifest.permission.CALL_PHONE}, CALL_REQUEST);
                    return;
                }
            }

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            activity.startActivity(callIntent);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    private void restart(){
        activity.finish();
        SharedPreferences pref = activity.getSharedPreferences("MyPref", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("chatDeleted",true);
        editor.commit();
    }
}