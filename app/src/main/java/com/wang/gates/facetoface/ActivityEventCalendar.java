package com.wang.gates.facetoface;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.PopupMenu;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;


public class ActivityEventCalendar extends AppCompatActivity {

    private static Chat chat;
    private static User user;

    public static CalendarView calendarView;
    private Button newEventButton;

    private static RecyclerView eventsRecyclerView;
    private LinearLayoutManager layoutManager;
    private static AdapterTitleContent eventsAdapter;
    private static ArrayList<Event> eventsArrayList = new ArrayList<>();
    private static AppCompatActivity activity;

    private static String patternDate = "yyyy-MM-dd";
    public static long dateLong;
    private HashMap<MenuItem, Integer> mapMenuItemIndex;
    //<key, index>

    private Menu mainMenu;

    private void getInfo() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.get("chat") != null) {
                //particular
                chat = (Chat) bundle.getSerializable("chat");
            }
            if (bundle.get("user") != null) {
                user = (User) getIntent().getExtras().get("user");
            }
            if (bundle.get("chat") == null) {
                //general
                chat = null;
                //hide button to make new event
                newEventButton.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void goToEvent() {
        Intent eventIntent = new Intent(ActivityEventCalendar.this, ActivityEvent.class);
        eventIntent.putExtra("eventKey", "new");//this indicates that we are creating a new event
        eventIntent.putExtra("dateLong", dateLong);
        eventIntent.putExtra("chat", chat);
        Bundle bundle = new Bundle();
        bundle.putSerializable("user", user);
        eventIntent.putExtras(bundle);
        //start activity for result
        startActivity(eventIntent);
    }//this is for creating new evens only

    public static void displayEventList() {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(dateLong);
        calendarView.setDate(dateLong, true, true);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patternDate);
        String dateString = simpleDateFormat.format(date.getTime());
        final String toSearchFor = dateString;

        //populate with events with the date selected
        final DatabaseReference events = FirebaseDatabase.getInstance().getReference("events");
        events.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                eventsArrayList.clear();
                for (DataSnapshot event : dataSnapshot.getChildren()) {
                    if (event.child("date").getValue().toString().equals(toSearchFor)) {
                        //make sure the chatKey matches
                        if (chat == null) {
                            DataSnapshot members = event.child("memberStatus");
                            for (DataSnapshot member : members.getChildren()) {
                                if (member.getKey().equals(user.getId())) {//make sure the user matches
                                    eventsArrayList.add((Event) event.getValue(Event.class));
                                }
                            }
                        } else if (chat != null && event.child("chatKey").getValue().toString().equals(chat.getChatKey())) {
                            eventsArrayList.add(event.getValue(Event.class));
                        }
                    }
                }
                LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
                layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                eventsRecyclerView.setLayoutManager(layoutManager);
                eventsAdapter = new AdapterTitleContent(eventsArrayList, activity);
                eventsRecyclerView.setAdapter(eventsAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_calendar);
        activity = this;
        //assign views
        calendarView = findViewById(R.id.calendarView);
        newEventButton = findViewById(R.id.new_event);
        eventsRecyclerView = findViewById(R.id.list_of_events);

        //set up recyclerview
        layoutManager = new LinearLayoutManager(this);
        eventsRecyclerView.setLayoutManager(layoutManager);
        eventsAdapter = new AdapterTitleContent(eventsArrayList, ActivityEventCalendar.this);
        eventsRecyclerView.setAdapter(eventsAdapter);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day, 0, 0);
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Calendar");
    }

    @Override
    protected void onStart() {
        super.onStart();
        displayEventList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activity = null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.calendar_menu, menu);
        mainMenu = menu;
        int menuItemId = 0;
        menu.findItem(R.id.selected_chat_name).getSubMenu().add(Menu.NONE, menuItemId, Menu.NONE, "All chats");
        mapMenuItemIndex = new HashMap<>();
        for(Chat chat: ActivityChatList.chatsArrayList){
            MenuItem menuItem = menu.findItem(R.id.selected_chat_name).getSubMenu().add(Menu.NONE, menuItemId, Menu.NONE, chat.getChatName());
            mapMenuItemIndex.put(menuItem, menuItemId);
            menuItemId++;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        if(mapMenuItemIndex.keySet().contains(item)){//if item is contained
            int index = mapMenuItemIndex.get(item);
            chat = ActivityChatList.chatsArrayList.get(index);
            newEventButton.setVisibility(View.VISIBLE);
            mainMenu.findItem(R.id.selected_chat_name).setTitle(chat.getChatName());
            displayEventList();
            return true;
        }
        else if(item.getTitle().toString().equals("All chats")){
            chat = null;
            mainMenu.findItem(R.id.selected_chat_name).setTitle("All chats");
            newEventButton.setVisibility(View.INVISIBLE);
            displayEventList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

