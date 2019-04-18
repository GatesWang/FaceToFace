package com.wang.gates.facetoface;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;

public class AdapterMember extends RecyclerView.Adapter<AdapterMember.ViewHolder> {
    private HashMap<String, String> values;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView eventName;
        public TextView eventTime;
        private final Context context;

        public View layout;

        public ViewHolder(View v) {
            super(v);
            context = v.getContext();
            layout = v;
            eventName = v.findViewById(R.id.row_event_name);
            eventTime = v.findViewById(R.id.row_event_time);
        }
    }

    public void add(int position, Event item) {

        notifyItemInserted(position);
    }

    public void remove(int position) {
        values.remove(position);
        notifyItemRemoved(position);
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public AdapterMember(HashMap<String, String> myDataset) {
        values = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdapterMember.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View v = inflater.inflate(R.layout.row_layout, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    //when clicked
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

    }

    @Override
    public int getItemCount() {
        return values.size();
    }

}