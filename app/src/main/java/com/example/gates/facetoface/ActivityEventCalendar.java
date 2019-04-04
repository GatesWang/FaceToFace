package com.example.gates.facetoface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;


import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class ActivityEventCalendar extends Activity{

    private FirebaseListAdapter<Event> mEventAdapter;
    private CalendarView mCalendar;
    private ListView mList;
    private FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_calendar);

        user = FirebaseAuth.getInstance().getCurrentUser();

        //assign views
        mCalendar = findViewById(R.id.calendarView);
        mList = findViewById(R.id.list_of_events);

        mCalendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView calendarView, int i, int i1, int i2) {
                String date = i1 + "/" + i2 + "/" + i;
                //populate the listview below
                changeListView(date);
            }
        });

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                DatabaseReference itemRef = mEventAdapter.getRef(position);
                String eventKey = itemRef.getKey();
                goToEvent(eventKey);
            }
        });

        Query query = FirebaseDatabase.getInstance().getReference().child("events");
        FirebaseListOptions<Event> options = new FirebaseListOptions.Builder<Event>()
                .setQuery(query, Event.class)
                .setLayout(R.layout.event)
                .build();

        mEventAdapter = new FirebaseListAdapter<Event>(options){
            @Override
            protected void populateView(View v, Event event, int position) {
                TextView eventName = (TextView)  v.findViewById(R.id.event_name);
                eventName.setText(event.toString());
            }
        };
        mList.setAdapter(mEventAdapter);
    }
    @Override
    protected void onStart() {
        super.onStart();
        mEventAdapter.startListening();
    }
    @Override
    protected void onStop() {
        super.onStop();
        mEventAdapter.stopListening();
    }

    private void goToEvent(String key){
        Intent i = new Intent(ActivityEventCalendar.this, ActivityEvent.class);
        i.putExtra("key", key);
        startActivity(i);
    }

    private void changeListView(String date){
        //changes the contents of mList based on the date provided and based on user
        //


    }

}

