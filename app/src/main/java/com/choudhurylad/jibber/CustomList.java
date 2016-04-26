package com.choudhurylad.jibber;

/**
 * Created by jannatul on 07/04/16.
 */
import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.parse.FindCallback;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.sinch.android.rtc.messaging.WritableMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomList extends ArrayAdapter<String> {
    private MainActivity activity;
    private ArrayList<String> friendList;
    private ArrayList<String> distance;

    public CustomList(MainActivity context, int resource, ArrayList<String> objects, ArrayList<String> distance) {
        super(context, resource, objects);
        this.activity = context;
        this.friendList = objects;
        this.distance = distance;
    }

    @Override
    public int getCount() {
        return friendList.size();
    }

    @Override
    public String getItem(int position) {
        return friendList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        // If holder not exist then locate all view from UI file.
        if (convertView == null) {
            // inflate UI from XML file
            convertView = inflater.inflate(R.layout.mylist, parent, false);
            // get all UI view
            holder = new ViewHolder(convertView);
            // set tag for holder
            convertView.setTag(holder);
        } else {
            // if holder created, get tag from view
            holder = (ViewHolder) convertView.getTag();
        }

        holder.friendName.setText(getItem(position));
        if (distance.get(position) == "Unavailable") {
            holder.extraTxt.setTextColor(Color.RED);
            holder.extraTxt.setText(distance.get(position));
        } else {
            holder.extraTxt.setTextColor(Color.BLACK);
            holder.extraTxt.setText(distance.get(position) + " miles");
        }

        //get first letter of each String item and generate colour
        String firstLetter = String.valueOf(getItem(position).charAt(0));
        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(getItem(position));
        TextDrawable drawable = TextDrawable.builder()
                .buildRound(firstLetter, color);
        holder.imageView.setImageDrawable(drawable);

        return convertView;
    }

    private class ViewHolder {
        private ImageView imageView;
        private TextView friendName;
        private TextView extraTxt;

        public ViewHolder(View v) {
            imageView = (ImageView) v.findViewById(R.id.image_view);
            friendName = (TextView) v.findViewById(R.id.item);
            extraTxt = (TextView) v.findViewById(R.id.textView1);
        }
    }
}
