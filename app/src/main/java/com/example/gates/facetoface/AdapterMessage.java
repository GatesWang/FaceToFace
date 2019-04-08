package com.example.gates.facetoface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;

public class AdapterMessage extends BaseAdapter implements ListAdapter {
    private ArrayList<ChatMessage> list = new ArrayList();
    private Context context;

    public AdapterMessage(ArrayList<ChatMessage> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
        //just return 0 if your list items do not have an Id variable.
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.message, null);
        }
        ChatMessage chatMessage = ((ChatMessage) list.get(position));
        String message = chatMessage.getMessageText();
        String user = chatMessage.getMessageUser();
        long time = chatMessage.getMessageTime();
        byte[] byteImage =  Base64.decode(chatMessage.getImage(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteImage, 0, byteImage.length);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        String date = "" + c.getTime();

        //start loading the views
        TextView messageTextView = (TextView)view.findViewById(R.id.message_text);
        ImageView userImage = (ImageView) view.findViewById(R.id.user_image);
        TextView userTextView = (TextView)view.findViewById(R.id.message_user);
        TextView timeTextView = (TextView)view.findViewById(R.id.message_time);

        userImage.setImageBitmap(bitmap);
        messageTextView.setText(message);
        userTextView.setText(user);
        timeTextView.setText(date);
        return view;
    }
}