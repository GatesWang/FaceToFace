package com.wang.gates.facetoface;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class AdapterMessage extends BaseAdapter implements ListAdapter {
    private HashMap<String, ChatMessage> map;
    //<id,message>
    private Context context;

    public AdapterMessage(HashMap<String, ChatMessage> map, Context context) {
        this.map = map;
        this.context = context;
    }

    @Override
    public int getCount() {
        if(map==null){
            return 0;
        }
        return map.size();
    }

    @Override
    public Object getItem(int pos) {
        ArrayList<String> indicies = new ArrayList<String>(map.keySet());
        Log.d(">>>", indicies + "");
        return map.get(indicies.get(pos));
    }

    @Override
    public long getItemId(int pos) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.message, null);
        }
        ArrayList<String> indiciesString = new ArrayList<>(map.keySet());
        HashMap<Integer,String> indicies = new HashMap<>();
        for(String sIndex: indiciesString){
            int index = Integer.parseInt(sIndex.substring(2));
            indicies.put(index, sIndex);
        }
        ChatMessage chatMessage = (ChatMessage) map.get(indicies.get(position+1));
        String message = chatMessage.getMessageText();
        String user = chatMessage.getMessageUser();
        String id = chatMessage.getUserID();
        long time = Long.parseLong(chatMessage.getMessageTime());

        //Log.d(">>>", "index " + index);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        String date = "" + c.getTime();

        //start loading the views
        TextView messageTextView = (TextView)view.findViewById(R.id.message_text);
        ImageView userImage = (ImageView) view.findViewById(R.id.user_image);
        TextView userTextView = (TextView)view.findViewById(R.id.message_user);
        TextView timeTextView = (TextView)view.findViewById(R.id.message_time);

        //set values for views
        //get the corresponding picture
        StorageReference profileRef = FirebaseStorage.getInstance().getReference("profile_pictures");
        StorageReference userRef = profileRef.child(id);
        GlideApp.with(context)
                .load(userRef)
                .into(userImage);

        messageTextView.setText(message);
        userTextView.setText(user);
        timeTextView.setText(date);
        return view;
    }
}