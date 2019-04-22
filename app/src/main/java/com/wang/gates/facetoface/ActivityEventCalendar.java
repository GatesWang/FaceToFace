package com.wang.gates.facetoface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


public class ActivityEventCalendar extends Activity{

    private Chat chat;
    private User user;

    private CalendarView calendarView;
    private Button newEventButton;

    private RecyclerView eventsRecyclerView;
    private LinearLayoutManager layoutManager;
    private AdapterEvent eventsAdapter;
    private ArrayList<Event> eventsArrayList = new ArrayList<>();

    private String patternTime = "hh:mm a";
    private String patternDate = "yyyy-MM-dd";
    private long dateLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_calendar);

        //assign views
        calendarView = findViewById(R.id.calendarView);
        newEventButton = findViewById(R.id.new_event);
        eventsRecyclerView = findViewById(R.id.list_of_events);

        //set up recyclerview
        layoutManager = new LinearLayoutManager(this);
        eventsRecyclerView.setLayoutManager(layoutManager);
        eventsAdapter = new AdapterEvent(eventsArrayList);
        eventsRecyclerView.setAdapter(eventsAdapter);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month , day, 0, 0);
                dateLong = calendar.getTimeInMillis();
                displayEventList();
            }
        });

        newEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToEvent();
            }
        });



        dateLong = calendarView.getDate();
        getInfo();
        displayEventList();
    }

    private void getInfo(){
        Bundle bundle = getIntent().getExtras();
        if(bundle!=null){
            if(bundle.get("chat")!=null){
                //particular
                chat = (Chat) bundle.getSerializable("chat");
            }
            if(bundle.get("user")!=null){
                user = (User) getIntent().getExtras().get("user");
                Log.d(">>>", "" + user.getId());
            }
        }
        else{
            //general
            chat = null;
            //hide button to make new event
            newEventButton.setVisibility(View.INVISIBLE);
        }
    }


    private void goToEvent(){
        Intent i = new Intent(ActivityEventCalendar.this, ActivityEvent.class);
        i.putExtra("eventKey","new");//this indicates that we are creating a new event
        i.putExtra("dateLong", dateLong);
        i.putExtra("chat", chat);
        startActivity(i);
    }

    private void displayEventList(){
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(dateLong);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patternDate);
        String dateString = simpleDateFormat.format(date.getTime());
        final String toSearchFor = "Event date: " + dateString;
        Log.d(">>>", toSearchFor);

        //populate with events with the date selected
        DatabaseReference events = FirebaseDatabase.getInstance().getReference("events");
        events.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                eventsArrayList.clear();
                eventsAdapter = new AdapterEvent(eventsArrayList);
                eventsRecyclerView.setAdapter(eventsAdapter);
                for(DataSnapshot event: dataSnapshot.getChildren()){
                    if(event.child("date").getValue().toString().equals(toSearchFor)){
                        //make sure the chatKey matches
                        if(chat==null){
                            DataSnapshot members = event.child("memberStatus");
                            for(DataSnapshot member: members.getChildren()){
                                if(member.getKey().toString().equals(user.getId())){//make sure the user matches
                                    eventsArrayList.add((Event) event.getValue(Event.class));
                                }
                            }
                        }
                        else if(chat!=null && event.child("chat").child("chatKey").getValue().toString().equals(chat.getChatKey())){
                            eventsArrayList.add((Event) event.getValue(Event.class));
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

