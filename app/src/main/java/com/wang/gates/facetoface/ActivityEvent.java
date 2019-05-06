package com.wang.gates.facetoface;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ActivityEvent extends AppCompatActivity {
    private Chat chat;
    private String eventKey;

    private long dateLong;
    private Calendar date;
    private Calendar time;
    private String patternTime = "hh:mm a";
    private String patternDate = "yyyy-MM-dd";
    private Event event;

    private Button eventDateView;
    private Button eventTimeView;
    private EditText eventNameView;
    private Button createEventButtonView;
    private Button removeEventButtonView;

    private RecyclerView statusRecyclerView;
    private LinearLayoutManager layoutManager;
    private AdapterStatus statusAdapter;
    private HashMap<String, Boolean> memberStatus = new HashMap<>();
    //<name, status>
    private HashMap<String, String> members = new HashMap<>();
    //<name, id>

    private String id;
    private DatabaseReference eventFirebase;
    private String[] onOff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        //get views
        eventDateView = findViewById(R.id.event_date);
        eventTimeView = findViewById(R.id.event_time);
        eventNameView = findViewById(R.id.new_event_name);
        createEventButtonView = findViewById(R.id.create_event_button);
        removeEventButtonView = findViewById(R.id.delete_event);

        statusRecyclerView  = findViewById(R.id.status_recycler);
        layoutManager = new LinearLayoutManager(ActivityEvent.this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        onOff = getResources().getStringArray(R.array.reminder_on_off);

        getInfo();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        eventTimeView.setOnClickListener(new View.OnClickListener() {
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
                saveEvent();
            }
        });
        removeEventButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteEvent();
            }
        });
        setUpNotifications();
    }
    private void setUpNotifications(){
        Spinner spinner = (Spinner) findViewById(R.id.reminder_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.reminder_on_off, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);


        final String reminderPreferenceKey;
        if(event==null){
            spinner.setSelection(getIndex(spinner, onOff[1]));
        }
        else{
            reminderPreferenceKey = event.getEventKey() + "reminders";
            final SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
            final SharedPreferences.Editor editor = pref.edit();
            boolean notify = pref.getBoolean(reminderPreferenceKey, false);
            if(notify){
                spinner.setSelection(getIndex(spinner, onOff[0]));
            }
            else{
                spinner.setSelection(getIndex(spinner, onOff[1]));
            }
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if(position==0){
                        //turn on
                        editor.putBoolean(reminderPreferenceKey, true);
                        editor.commit();
                    }
                    else if(position == 1){
                        //turn off
                        editor.putBoolean(reminderPreferenceKey, false);
                        editor.commit();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) { }
            });
        }
    }
    private int getIndex(Spinner spinner, String myString){
        int index = 0;
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).equals(myString)){
                index = i;
            }
        }
        return index;
    }
    @Override
    protected void onStart() {
        super.onStart();
    }
    private void getInfo(){
        Intent i = getIntent();
        if(i.getSerializableExtra("event") != null){//the event already exists
            event = (Event)i.getSerializableExtra("event");
            //get reference to chat object
            final String chatKey = event.getChatKey();
            DatabaseReference members = FirebaseDatabase.getInstance().getReference().child("members");
            members.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot chatJson: dataSnapshot.getChildren()){
                        if(chatJson.child("chatKey").getValue().toString().equals(chatKey)){
                            chat = chatJson.getValue(Chat.class);
                            getSupportActionBar().setTitle("" + chat);
                            fillViews();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            eventKey = event.getEventKey();
        }
        else{//event doesnt exist yet
            chat = (Chat) i.getSerializableExtra("chat");
            dateLong = i.getLongExtra("dateLong", 0);
            eventKey = i.getStringExtra("eventKey");
            fillViews();
        }
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        id = pref.getString("id", null);
    }
    private void fillViews(){
        populateMemberStatus();
        if(event!=null){
            eventDateView.setText(event.getDate());
            eventTimeView.setText(event.getTime());
            eventNameView.setText(event.getEventName());
        }
        else{//new event
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
    private void saveEvent(){
        //create Event object from views
        String eventName = eventNameView.getText().toString().trim();
        String dateString = eventDateView.getText().toString();
        String timeString = eventTimeView.getText().toString();
        String dateSubstring = dateString.substring(12);
        String timeSubstring = timeString.substring(12);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
        Calendar cal = Calendar.getInstance();
        try{
            Date date = sdf.parse(dateSubstring+ " " + timeSubstring);
            cal.setTime(date);
        }
        catch (Exception e){
            Log.d(">>>",e.toString());
        }

        if(eventName.length()>=1){
            final Event event = new Event(eventName, dateString, timeString, memberStatus, chat.getChatKey());
            String reminderPreferenceKeyTemp = "";
            if(this.event!=null){
                event.setEventKey(this.event.getEventKey());
                final DatabaseReference database = FirebaseDatabase.getInstance().getReference("events");
                database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot e : dataSnapshot.getChildren()){
                            if(e.child("eventKey").getValue().toString().equals(event.getEventKey())){
                                database.child(e.getKey()).setValue(event);
                                ActivityEventCalendar.displayEventList();
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                reminderPreferenceKeyTemp = event.getEventKey() + "reminders";
            }
            else{
                //creating event for the first time
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("events");
                eventFirebase = database.push();
                event.setEventKey(eventFirebase.getKey());
                database.push().setValue(event);
                reminderPreferenceKeyTemp = eventFirebase.getKey() + "reminders";
            }
            //write sharedpreferences for reminder
            final String reminderPreferenceKey = reminderPreferenceKeyTemp;
            Spinner spinner = (Spinner) findViewById(R.id.reminder_spinner);
            final SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
            final SharedPreferences.Editor editor = pref.edit();
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if(position==0){
                        //turn on
                        editor.putBoolean(reminderPreferenceKey, true);
                        editor.commit();
                    }
                    else if(position == 1){
                        //turn off
                        editor.putBoolean(reminderPreferenceKey, false);
                        editor.commit();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) { }
            });

            //create reminders
            boolean notify = pref.getBoolean(reminderPreferenceKey, false);

            if(notify){
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra("beginTime", cal.getTimeInMillis());
                intent.putExtra("allDay", false);
                intent.putExtra("title", event.getEventName());
                startActivity(intent);
            }
            statusAdapter.updateFirebase();
            ActivityEventCalendar.displayEventList();
            finish();
        }
        else{
            Toast.makeText(this, "Event name is invalid", Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteEvent(){
        if(this.event!=null){
            final DatabaseReference database = FirebaseDatabase.getInstance().getReference("events");
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot e : dataSnapshot.getChildren()){
                        if(e.child("eventKey").getValue().toString().equals(event.getEventKey())){
                            database.child(e.getKey()).setValue(null);
                        }
                    }
                    ActivityEventCalendar.displayEventList();
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else{
            ActivityEventCalendar.displayEventList();
            finish();
        }
    }
    private void populateMemberStatus(){
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("members").child(chat.getChatKey()).child("memberIds");
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(final DataSnapshot member: dataSnapshot.getChildren()){
                    memberStatus.put(member.getValue().toString(), false);
                    //<id, status>

                    //fills out members
                    final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            ArrayList<String> keys = new ArrayList<>(memberStatus.keySet());
                            for(DataSnapshot user: dataSnapshot.getChildren()){
                                if(keys.contains(user.getKey())){
                                    members.put(user.getKey(), user.child("name").getValue().toString());
                                }
                            }
                            getStatus(); //updates recyclerview inside
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void updateRecyclerView(){
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        statusRecyclerView.setLayoutManager(layoutManager);
        statusAdapter = new AdapterStatus(memberStatus, members, id, event.getEventKey());
        statusRecyclerView.setAdapter(statusAdapter);
    }
    private void getStatus(){
        if(eventKey.equals("new")){

        }
        else{
            DatabaseReference eventsRef = FirebaseDatabase.getInstance().getReference("events");
            eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot event: dataSnapshot.getChildren()){
                        if(event.child("eventKey").getValue().toString().equals(eventKey)){
                            HashMap<String,Boolean> membersStatusTemp = (HashMap) event.child("memberStatus").getValue();
                            for(String s: membersStatusTemp.keySet()){
                                memberStatus.put(s,membersStatusTemp.get(s));
                            }
                            updateRecyclerView();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

