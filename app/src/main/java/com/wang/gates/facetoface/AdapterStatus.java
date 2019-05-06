package com.wang.gates.facetoface;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;

public class AdapterStatus extends RecyclerView.Adapter<AdapterStatus.ViewHolder> {
    private HashMap<String, Boolean> memberStatus;
    //<id, status>
    private HashMap<String, String> members;
    //<id, name>
    private String mId;
    private String eventKey;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView memberName;
        public ImageView memberPicture;
        public Switch memberSwitchStatus;

        private final Context context;

        public View layout;

        public ViewHolder(View v) {
            super(v);
            context = v.getContext();
            layout = v;
            memberName = v.findViewById(R.id.status_member_name);
            memberPicture = v.findViewById(R.id.status_member_picture);
            memberSwitchStatus = v.findViewById(R.id.status_switch);
        }
    }


    public AdapterStatus(HashMap<String, Boolean> memberStatus, HashMap<String, String> members, String id, String eventKey)
    {
        this.memberStatus = memberStatus;
        this.members = members;
        this.mId = id;
        this.eventKey = eventKey;
    }

    @Override
    public AdapterStatus.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.status_row_layout, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        String id = (String) members.keySet().toArray()[position];
        final String name = members.get(id);
        holder.memberName.setText(name);

        StorageReference profileRef = FirebaseStorage.getInstance().getReference("profile_pictures");
        StorageReference userRef = profileRef.child(id);

        GlideApp.with(holder.context)
                .load(userRef)
                .into(holder.memberPicture);

        Boolean status = memberStatus.get(id);
        if(status==true){
            holder.memberSwitchStatus.setChecked(true);
            holder.memberSwitchStatus.setText("Going");
        }

        if(!id.equals(mId)){
            holder.memberSwitchStatus.setClickable(false);
        }
        holder.memberSwitchStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked==false){
                    holder.memberSwitchStatus.setChecked(false);
                    holder.memberSwitchStatus.setText("Not Going");
                    memberStatus.put(mId, false);
                }
                else{
                    holder.memberSwitchStatus.setChecked(true);
                    holder.memberSwitchStatus.setText("Going");
                    memberStatus.put(mId, true);
                }
            }
        });
    }


    public void updateFirebase(){
        //send to firebase
        final DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference().child("events");
        //loop through all events to find the one
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot event: dataSnapshot.getChildren()){
                    if(event.child("eventKey").getValue().toString().equals(eventKey)){
                        eventsRef.child(event.getKey()).child("memberStatus").child(mId).setValue(memberStatus.get(mId));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return memberStatus.size();
    }
}