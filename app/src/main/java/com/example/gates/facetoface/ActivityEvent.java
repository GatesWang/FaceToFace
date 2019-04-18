package com.example.gates.facetoface;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class ActivityEvent extends Activity {
    private Chat chat;
    private String eventKey;

    private long dateLong;
    private Calendar date;
    private Calendar time;
    private String patternTime = "hh:mm a";
    private String patternDate = "yyyy-MM-dd";
    private Event event;

    private TextView chatName;
    private TextView eventDateView;
    private TextView eventTimeView;
    private EditText eventNameView;
    private Button setTimeButtonView;
    private Button createEventButtonView;

    private HashMap<String, Boolean> memberStatus = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        //get views
        chatName = findViewById(R.id.event_chat_name);
        eventDateView = findViewById(R.id.event__date);
        eventTimeView = findViewById(R.id.event__time);
        eventNameView = findViewById(R.id.new_event_name);
        setTimeButtonView = findViewById(R.id.set_event_time_button);
        createEventButtonView = findViewById(R.id.create_event_button);

        getInfo();
        populateMemberStatus();
        fillViews();

        setTimeButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(ActivityEvent.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        String patternTime = "hh:mm a";
                        SimpleDateFormat simpleDateFormatTime = new SimpleDateFormat(patternTime);
                        String timeString = simpleDateFormatTime.format(calendar.getTime());
                        eventTimeView.setText("Event time: " + timeString);
                    }
                }, 12, 00, false);
                timePickerDialog.show();
            }
        });

        createEventButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToFirebase();
            }
        });
    }

    private void getInfo(){
        Intent i = getIntent();
        if(i.getSerializableExtra("event") != null){//passing an events object in instead
            event = (Event)i.getSerializableExtra("event");
            chat = event.getChat();
        }
        else{
            chat = (Chat) i.getSerializableExtra("chat");
            dateLong = i.getLongExtra("dateLong", 0);
            eventKey = i.getStringExtra("eventKey");
        }
    }
    private void fillViews(){
        if(event!=null){
            chatName.setText(chat.getChatName());
            eventDateView.setText(event.getDate());
            eventTimeView.setText(event.getTime());
            eventNameView.setText(event.getEventName());
        }
        else{
            chatName.setText(chat.getChatName());
            date = Calendar.getInstance();
            date.setTimeInMillis(dateLong);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patternDate);
            String dateString = simpleDateFormat.format(date.getTime());
            eventDateView.setText("Event date: " + dateString);

            if(eventKey.equals("new")){//new event
                //time
                time = Calendar.getInstance();
                SimpleDateFormat simpleDateFormatTime = new SimpleDateFormat(patternTime);
                String timeString = simpleDateFormatTime.format(time.getTime());
                eventTimeView.setText("Event time: " + timeString);
                //name
                eventNameView.setText("New event");
            }
        }
    }
    private void saveToFirebase(){
        //create Event object from views
        String eventName = eventNameView.getText().toString();
        String date = eventDateView.getText().toString();
        String time = eventTimeView.getText().toString();

        final Event event = new Event(eventName, date, time, memberStatus, chat);
        event.setEventKey(this.event.getEventKey());
        if(event!=null){
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference("events");
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot e : dataSnapshot.getChildren()){
                        Log.d(">>>", "1 " + e.child("eventKey").getValue().toString());
                        Log.d(">>>", "2 " + event.getEventKey());

                        if(e.child("eventKey").getValue().toString().equals(event.getEventKey())){
                            database.child(e.getKey()).setValue(event);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else{
            DatabaseReference database = FirebaseDatabase.getInstance().getReference("events");
            DatabaseReference eventFirebase = database.push();
            event.setEventKey(eventFirebase.getKey());
            database.push().setValue(event);
            finish();
        }
    }
    private void populateMemberStatus(){
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("members").child(chat.getChatKey()).child("memberIds");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot member: dataSnapshot.getChildren()){
                    memberStatus.put(member.getValue().toString(), false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}

