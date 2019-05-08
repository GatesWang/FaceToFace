package com.wang.gates.facetoface;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdapterTitleContent extends RecyclerView.Adapter<AdapterTitleContent.ViewHolder> {
    private List values;
    private Context context;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView title;
        public TextView content;
        private final Context context;

        public View layout;

        public ViewHolder(View v) {
            super(v);
            context = v.getContext();
            layout = v;
            itemView.setOnClickListener(this);
            title = v.findViewById(R.id.title);
            content = v.findViewById(R.id.content);
        }

        @Override
        public void onClick(View v) {
            if(!values.isEmpty()){
                if(values.get(0) instanceof Chat){
                    Intent intent = new Intent(context, ActivityChat.class);
                    intent.putExtra("person", ((ActivityChatList)context).getPerson());
                    Chat chat = (Chat) values.get(getAdapterPosition());
                    intent.putExtra("chat", chat);
                    Bundle chatList = new Bundle();
                    chatList.putSerializable("chatList", ((ActivityChatList)context).getChatList());
                    intent.putExtras(chatList);
                    context.startActivity(intent);
                }
                else if(values.get(0) instanceof Event){
                    Intent i = new Intent(context, ActivityEvent.class);
                    Bundle bundle = new Bundle();
                    Event event = (Event) values.get(getAdapterPosition());
                    bundle.putSerializable("event", event);
                    i.putExtras(bundle);
                    context.startActivity(i);
                }
            }
        }
    }

    public void add(int position, Object item) {
        values.add(position, item);
        notifyItemInserted(position);
    }

    public AdapterTitleContent(List myDataset, Context context) {
        values = myDataset;
        this.context = context;

        if(values.size()>0 && values.get(0) instanceof Event){
            //sort the events
            Collections.sort(myDataset, new Comparator<Event>() {
                @Override
                public int compare(Event lhs, Event rhs) {
                    String patternTime = "hh:mm a";
                    SimpleDateFormat sdf = new SimpleDateFormat(patternTime);
                    try{
                        Calendar left = Calendar.getInstance();
                        Calendar right = Calendar.getInstance();
                        left.setTime(sdf.parse(lhs.getTime()));
                        right.setTime(sdf.parse(rhs.getTime()));
                        return left.getTimeInMillis() < right.getTimeInMillis() ? -1 : left.getTimeInMillis() > right.getTimeInMillis() ? 1 : 0;
                    }
                    catch(Exception e){

                    }
                    return 0;
                }
            });
            notifyDataSetChanged();
        }
    }

    @Override
    public AdapterTitleContent.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View v = inflater.inflate(R.layout.row_layout, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        holder.itemView.setLongClickable(true);
        Object object = values.get(position);

        if(object instanceof Event){
            final Event event = (Event) values.get(position);
            holder.title.setText(event.getEventName());
            holder.content.setText(event.getTime());
        }

        else if(object instanceof Chat){
            final Chat chat = (Chat) values.get(position);
            holder.title.setText(chat.getChatName());
            holder.title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
            DatabaseReference messages = FirebaseDatabase.getInstance().getReference().child("messages");
            messages.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot chatJson: dataSnapshot.getChildren()){
                        if(chatJson.child("chatId").getValue().toString().equals(chat.getChatKey())){//for the chat we want
                            if(!chatJson.child("messageId").getValue().toString().equals("ID0")){//at least one message
                                String messageId = chatJson.child("messageId").getValue().toString();
                                holder.content.setText(chatJson.child("messages").child(messageId).getValue(ChatMessage.class).toString());
                                //Log.d(">>>", chat.child("messages").child(messageId).getValue().toString());
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            }
        }


    @Override
    public int getItemCount() {
        return values.size();
    }
}

