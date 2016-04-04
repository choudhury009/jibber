package com.choudhurylad.jibber;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.PushService;
import com.parse.SaveCallback;
import com.sinch.android.rtc.messaging.Message;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.WritableMessage;

import org.json.JSONObject;

import java.text.CollationElementIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUsersActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Intent intent;
    private String currentUserId;
    private ArrayAdapter<String> namesArrayAdapter;
    private ArrayList<String> names;
    private ListView usersListView;
    private ProgressDialog progressDialog;
    private BroadcastReceiver receiver = null;
    private SwipeRefreshLayout swipeRefreshLayout;
    private GoogleApiClient mGoogleApiClient;
    private CollationElementIterator mLatitudeText;
    private CollationElementIterator mLongitudeText;
    private Location mLastLocation;
    private ParseGeoPoint point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_users);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_child_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        String currentUsername = ParseUser.getCurrentUser().getUsername();
        TextView tv1 = (TextView)findViewById(R.id.currentUsername);
        tv1.setText("Hello " + currentUsername);

//        showSpinner();
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        // enables push
        installation.put("GCMSenderId", "896369439262");
        installation.saveInBackground();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        findViewById(R.id.jibberSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findMatch();
            }
        });
        findViewById(R.id.chatList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent = new Intent(getApplicationContext(), ListUsersActivity.class);
                startActivity(intent);
            }
        });
    }


    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
//            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
//            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            Toast.makeText(getApplicationContext(),String.valueOf(mLastLocation.getLongitude()), Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(),String.valueOf(mLastLocation.getLatitude()), Toast.LENGTH_LONG).show();
            ParseUser user = ParseUser.getCurrentUser();
            point = new ParseGeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            user.put("location", point);
            user.saveInBackground();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    public void findMatch() {
        currentUserId = ParseUser.getCurrentUser().getObjectId();
        names = new ArrayList<String>();
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", currentUserId);
        query.whereWithinMiles("location", point, 1.0);

        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    String gender = "male";
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getObjectId().toString().equals(currentUserId)) {
                            if (!userList.get(i).getString("read").toString().equals("male")) {
                                // user is female
                                gender = "female";
                            } else {
                                gender = "male";
                            }
                        } else {
                            if (gender == "male") {
                                if (userList.get(i).getString("read").toString().equals("female")) {
                                    names.add(userList.get(i).getUsername().toString());
                                }
                            } else {
                                if (userList.get(i).getString("read").toString().equals("male")) {
                                    names.add(userList.get(i).getUsername().toString());
                                }
                            }
                        }
                    }
                    usersListView = (ListView) findViewById(R.id.usersListView);
                    namesArrayAdapter =
                            new ArrayAdapter<String>(getApplicationContext(),
                                    R.layout.user_list_item, names);
                    usersListView.setAdapter(namesArrayAdapter);
                    swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe);
                    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

                        @Override
                        public void onRefresh() {
                            swipeRefreshLayout.setRefreshing(true);
                            (new Handler()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    findMatch();
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }, 3000);
                        }
                    });
                    swipeRefreshLayout.setColorScheme(android.R.color.holo_blue_dark, android.R.color.holo_blue_light, android.R.color.holo_green_light, android.R.color.holo_green_dark);
                    usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                            openConversation(names, i);
                        }
                    });

                } else {
                    Toast.makeText(getApplicationContext(), "Error loading user list", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //display clickable a list of all users
    private void setConversationsList() {
        currentUserId = ParseUser.getCurrentUser().getObjectId();
        names = new ArrayList<String>();
        ParseQuery<ParseUser> query = ParseUser.getQuery();
//        query.whereNotEqualTo("objectId", currentUserId);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    String allUsers = "";
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getObjectId().toString().equals(currentUserId)) {
                            if (!userList.get(i).getString("read").toString().equals("read")) {
                                allUsers += " " + userList.get(i).getString("read").toString();
                            } else {
                                // remove username from read
                            }
                        } else {
                            names.add(userList.get(i).getUsername().toString());
                        }
                    }
//                    if (allUsers.length() > 0) {
//                        Toast.makeText(getApplicationContext(),"Unread message from " + allUsers, Toast.LENGTH_LONG).show();
//                    }
                    usersListView = (ListView) findViewById(R.id.usersListView);
                    namesArrayAdapter =
                            new ArrayAdapter<String>(getApplicationContext(),
                                    R.layout.user_list_item, names);
                    usersListView.setAdapter(namesArrayAdapter);
                    swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe);
                    // the refresh listner. this would be called when the layout is pulled down
                    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

                        @Override
                        public void onRefresh() {
                            // get the new data from you data source
                            swipeRefreshLayout.setRefreshing(true);
                            (new Handler()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setConversationsList();
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            }, 3000);
                        }
                    });
                    swipeRefreshLayout.setColorScheme(android.R.color.holo_blue_dark, android.R.color.holo_blue_light, android.R.color.holo_green_light, android.R.color.holo_green_dark);
//                    registerForContextMenu(usersListView);
                    usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> a, View v, int i, long l) {
//                            PopupMenu popup = new PopupMenu(getApplicationContext(),v);
//                            popup.getMenuInflater().inflate(R.menu.search_popup, popup.getMenu());
//                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//                                @Override
//                                public boolean onMenuItemClick(MenuItem item) {
//                                    Toast.makeText(getApplicationContext(), "Request sent", Toast.LENGTH_LONG).show();
//                                    return true;
//                                }
//                            });
//                            popup.show();
                            openConversation(names, i);
                        }
                    });

                } else {
                    Toast.makeText(getApplicationContext(), "Error loading user list", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    //open a conversation with one person
    public void openConversation(ArrayList<String> names, int pos) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", names.get(pos));
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> user, com.parse.ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(getApplicationContext(), MessagingActivity.class);
                    intent.putExtra("RECIPIENT_ID", user.get(0).getObjectId());
                    intent.putExtra("MESSAGE_USERNAME", user.get(0).getUsername());
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error finding that user",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //show a loading spinner while the sinch client starts
    private void showSpinner() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Boolean success = intent.getBooleanExtra("success", false);
                progressDialog.dismiss();
                if (!success) {
                    Toast.makeText(getApplicationContext(), "Messaging service failed to start", Toast.LENGTH_LONG).show();
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("com.choudhurylad.jibber.ListUsersActivity"));
    }

    @Override
    public void onResume() {
        setConversationsList();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.list_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                intent = new Intent(getApplicationContext(), DetailsActivity.class);
                startActivity(intent);
                return true;

            case R.id.action_logout:
                stopService(new Intent(getApplicationContext(), MessageService.class));
                ParseUser.logOut();
                intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}



