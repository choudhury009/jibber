package com.choudhurylad.jibber;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestAsyncTask;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.ProfilePictureView;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseException;
//import com.parse.ParseFacebookUtils;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Intent intent;
    private Intent serviceIntent;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String currentUserId;
    private ArrayAdapter<String> namesArrayAdapter;
    private ArrayList<String> names;
    private ArrayList<String> nearbyNames;
    private ArrayList<String> nearbyFacebookId;
    private ListView usersListViewFrag;
    private ListView searchUsersListViewFrag;
    private ParseGeoPoint point;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View jibberSearch;
    private ParseUser currentUser;
    private String facebookId;
    private AdView mAdView;
    private int picCount;

    @Override
    // TODO: 03/02/16 create settings page to allow users to set their match type first before searching
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        // enables push
        installation.put("GCMSenderId", "896369439262");
        installation.saveInBackground();
        FacebookSdk.sdkInitialize(getApplicationContext());

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        ParseUser user = ParseUser.getCurrentUser();
        user.put("unread", false);
        user.saveInBackground();
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        currentUser = ParseUser.getCurrentUser();

        // not needed as facebook login is compulsary
//        if (!ParseFacebookUtils.isLinked(currentUser)) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//            builder.setTitle("Jibber")
//                    .setMessage("Please link your Facebook account")
//                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            // User clicked OK button
//                            List<String> permissions = Arrays.asList("basic_info", "user_about_me",
//                                    "user_relationships", "user_birthday", "user_location", "email");
//                            ParseFacebookUtils.linkWithReadPermissionsInBackground(currentUser, MainActivity.this, null, new SaveCallback() {
//                                @Override
//                                public void done(ParseException ex) {
//                                    if (ParseFacebookUtils.isLinked(currentUser)) {
//                                        Log.d("MyApp", "Woohoo, user logged in with Facebook!");
//                                    } else {
//                                        Log.d("MyApp", "user cancelled");
//                                    }
//                                }
//                            });
//                        }
//                    })
//                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            // User cancelled the dialog
//                            dialog.cancel();
//                        }
//                    });
//            AlertDialog dialog = builder.create();
//            dialog.show();
//        }

        // define the tab layout and set title for each tab
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("CHATS"));
        tabLayout.addTab(tabLayout.newTab().setText("SEARCH"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                if (tab.getText() == "SEARCH") {
                    GraphRequestAsyncTask request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject user, GraphResponse response) {
                            if (user != null) {
                                // set the profile picture using their Facebook ID
                                ProfilePictureView profilePic = (ProfilePictureView) findViewById(R.id.myProfilePic1);
                                profilePic.setProfileId(user.optString("id"));
                                facebookId = user.optString("id");
                            }
                        }
                    }).executeAsync();
                    if (facebookId != null) {
                        currentUser.put("facebookId", facebookId);
                        currentUser.saveInBackground();
                    }
                    // if search has been made then show the listview first
                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("objectId", currentUserId);

                    query.findInBackground(new FindCallback<ParseUser>() {
                        public void done(List<ParseUser> userList, com.parse.ParseException e) {
                            if (e == null) {
                                // false
                                searchUsersListViewFrag = (ListView) findViewById(R.id.searchUsersListViewFrag);
                                jibberSearch = findViewById(R.id.jibberSearch);
                                if (userList.get(0).get("unread").equals(true)) {
                                    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                        jibberSearch.animate().translationY(-900).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animationBtn) {
                                                super.onAnimationEnd(animationBtn);
                                                searchUsersListViewFrag.animate().translationY(250).setListener(new AnimatorListenerAdapter() {
                                                    @Override
                                                    public void onAnimationEnd(Animator animation) {
                                                        super.onAnimationEnd(animation);
//                                                        searchUsersListViewFrag.setVisibility(View.VISIBLE);
                                                        Toast.makeText(getApplicationContext(), "visible", Toast.LENGTH_LONG).show();
                                                        jibberSearchClick();
                                                    }
                                                });
                                            }
                                        });
                                    } else {
                                        searchUsersListViewFrag.setVisibility(View.GONE);
                                        jibberSearch.animate().translationY(0);
                                        jibberSearchClick();
                                    }
                                } else {
                                    searchUsersListViewFrag.setVisibility(View.GONE);
                                    jibberSearch.animate().translationY(0);
                                    jibberSearchClick();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Failed to retrieve data", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
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
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
        findMatch();
    }

    public void listUsersActivity() {
        intent = new Intent(getApplicationContext(), ListUsersActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_listusers:
                listUsersActivity();

                return true;
            case R.id.action_logout:
                ParseUser.logOut();
                intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void jibberSearchClick() {
        jibberSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    jibberSearch.animate().translationY(-900).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            searchUsersListViewFrag.animate().translationY(250).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
//                                    searchUsersListViewFrag.setVisibility(View.VISIBLE);
                                    Toast.makeText(getApplicationContext(),
                                            "Searching for nearby people",
                                            Toast.LENGTH_LONG).show();
                                    nearbyPeople();
                                }
                            });
                        }
                    });
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Enable GPS")
                            .setMessage("Please turn on location services.")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button
                                    Intent gpsOptionsIntent = new Intent(
                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    startActivity(gpsOptionsIntent);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                    dialog.cancel();
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });
    }

    public void findMatch() {
        currentUserId = ParseUser.getCurrentUser().getObjectId();
        names = new ArrayList<String>();
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", currentUserId);
//        query.whereWithinMiles("location", point, 1.0);
        // where chats have been created for other user e.g. array of chats

        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    String gender = "male";
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getObjectId().toString().equals(currentUserId)) {
//                            if (!userList.get(i).getString("read").toString().equals("male")) {
//                                gender = "female";
//                            } else {
//                                gender = "male";
//                            }
                        } else {
                            names.add(userList.get(i).getUsername().toString());
//                            if (gender == "male") {
//                                if (userList.get(i).getString("read").toString().equals("female")) {
//                                    names.add(userList.get(i).getUsername().toString());
//                                }
//                            } else {
//                                if (userList.get(i).getString("read").toString().equals("male")) {
//                                    names.add(userList.get(i).getUsername().toString());
//                                }
//                            }
                        }
                    }
                    usersListViewFrag = (ListView) findViewById(R.id.usersListViewFrag);
                    namesArrayAdapter =
                            new ArrayAdapter<String>(getApplicationContext(),
                                    R.layout.user_list_item, names);
                    usersListViewFrag.setAdapter(namesArrayAdapter);
                    usersListViewFrag.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                            openConversation(names, i);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "Unable to retrieve data", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void nearbyPeople() {
        currentUserId = ParseUser.getCurrentUser().getObjectId();
        nearbyNames = new ArrayList<String>();
        nearbyFacebookId = new ArrayList<String>();

        ParseQuery<ParseUser> thisUser = ParseUser.getQuery();
        thisUser.whereEqualTo("objectId",currentUserId);
        thisUser.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    point = userList.get(0).getParseGeoPoint("location");
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Unable to retrieve location, please try again.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", currentUserId);
        query.whereWithinMiles("location", point, 0.5);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getObjectId().toString().equals(currentUserId)) {
                        } else {
                            if (userList.get(i).get("facebookId").toString() != null) {
                                nearbyFacebookId.add(userList.get(i).get("facebookId").toString() );
                            }
                            nearbyNames.add(userList.get(i).getUsername().toString());
                        }
                    }
                    for(picCount = 0; picCount<nearbyFacebookId.size(); picCount++) {
                        // set the profile picture using their Facebook ID
                        if (picCount == 0) {
                            ProfilePictureView profilePic1 = (ProfilePictureView) findViewById(R.id.myProfilePic2);
                            profilePic1.setProfileId(nearbyFacebookId.get(picCount).toString());
                            Log.d("facebook", nearbyFacebookId.get(picCount).toString());
                            profilePic1.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    // your code here
                                    openConversation(nearbyNames, 0);
                                }
                            });
                        }
                        if (picCount == 1) {
                            ProfilePictureView profilePic2 = (ProfilePictureView) findViewById(R.id.myProfilePic3);
                            profilePic2.setProfileId(nearbyFacebookId.get(picCount).toString());
                            Log.d("facebook", nearbyFacebookId.get(picCount).toString());
                            profilePic2.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    // your code here
                                    openConversation(nearbyNames, 1);
                                }
                            });
                        }
                    }
//                    searchUsersListViewFrag = (ListView) findViewById(R.id.searchUsersListViewFrag);
//                    namesArrayAdapter =
//                            new ArrayAdapter<String>(getApplicationContext(),
//                                    R.layout.user_list_item, nearbyNames);
//                    searchUsersListViewFrag.setAdapter(namesArrayAdapter);
//                    searchUsersListViewFrag.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                        @Override
//                        public void onItemClick(AdapterView<?> a, View v, int i, long l) {
//                            openConversation(nearbyNames, i);
//                        }
//                    });
                    ParseUser user = ParseUser.getCurrentUser();
                    user.put("unread", true);
                    user.saveInBackground();
                } else {
                    Toast.makeText(getApplicationContext(), "Unable to retrieve from search", Toast.LENGTH_LONG).show();
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

    @Override
    public void onBackPressed()
    {
        moveTaskToBack(true); // exit app
    }

}
