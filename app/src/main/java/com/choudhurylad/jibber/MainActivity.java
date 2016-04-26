package com.choudhurylad.jibber;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.facebook.FacebookSdk;
import com.facebook.login.widget.ProfilePictureView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseFacebookUtils;
import com.parse.ParseGeoPoint;
import com.parse.ParseInstallation;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Intent intent;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String currentUserId;
    private ArrayList<String> names;
    private ArrayList<String> nearbyNames;
    private ArrayList<String> nearbyFacebookId;
    private ParseGeoPoint point;
    private View jibberSearch;
//    private AdView mAdView;
    private int picCount;
    ProfilePictureView pic1;
    ProfilePictureView pic2;
    ProfilePictureView pic3;
    ProfilePictureView pic4;
    private ArrayList<String> distance;
    private boolean searching;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.put("user", ParseUser.getCurrentUser());
        // enables push
        installation.put("GCMSenderId", "896369439262");
        installation.saveInBackground();
        FacebookSdk.sdkInitialize(getApplicationContext());

//        mAdView = (AdView) findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);

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
                    pic1 = (ProfilePictureView) findViewById(R.id.myProfilePic1);
                    pic2 = (ProfilePictureView) findViewById(R.id.myProfilePic2);
                    pic3 = (ProfilePictureView) findViewById(R.id.myProfilePic3);
                    pic4 = (ProfilePictureView) findViewById(R.id.myProfilePic4);

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("objectId", currentUserId);
                    query.findInBackground(new FindCallback<ParseUser>() {
                        public void done(List<ParseUser> userList, com.parse.ParseException e) {
                            if (e == null) {
                                // false
                                jibberSearch = findViewById(R.id.jibberSearch);
                                if (searching != true) {
                                    jibberSearch.animate().translationY(0);
                                }
                                jibberSearch.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                                        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                            DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
                                            int height = displayMetrics.heightPixels;
                                            int yVal = (height/2) - 400;
                                            jibberSearch.animate().translationY(yVal).setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    super.onAnimationEnd(animation);
                                                    Toast.makeText(getApplicationContext(), "Searching...", Toast.LENGTH_SHORT).show();
                                                    searching = true;
                                                    nearbyPeople();
                                                    findMatch();
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
                            } else {
                                Toast.makeText(getApplicationContext(), "No data retrieved", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
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
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onConnectionSuspended(int i) {}

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
            case R.id.action_logout:
                ParseUser.logOut();
                intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void findMatch() {
        currentUserId = ParseUser.getCurrentUser().getObjectId();
        distance = new ArrayList<String>();
        names = new ArrayList<String>();
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", currentUserId);
        query.whereNotEqualTo("objectId", "2HDU94lVzB");
//        query.whereWithinMiles("location", point, 1.0);
        // where chats have been created for other user e.g. array of chats

        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    String gender = "male";
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getObjectId().toString().equals(currentUserId)) {
                        } else {
                            names.add(userList.get(i).getUsername().toString());
                            if (point != null) {
                                Object coordinates = userList.get(i).get("location");
                                ParseGeoPoint obj = (ParseGeoPoint) coordinates;
                                if (obj != null) {
                                    Double difference = point.distanceInMilesTo(obj);
                                    String newDistance = String.format("%.2f", difference);
                                    distance.add(newDistance);
                                } else {
                                    distance.add("Unavailable");
                                }
                            } else {
                                distance.add("enable gps for");
                            }
                        }
                    }
                    CustomList adapter = new CustomList(MainActivity.this, R.layout.mylist, names, distance);
                    ListView list = (ListView) findViewById(R.id.usersListViewFrag);
                    list.setAdapter(adapter);

                    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            openConversation(names, position);
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "No data retrieved", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void nearbyPeople() {
        currentUserId = ParseUser.getCurrentUser().getObjectId();
        nearbyNames = new ArrayList<String>();
        nearbyFacebookId = new ArrayList<String>();

        ParseQuery<ParseUser> thisUser = ParseUser.getQuery();
        thisUser.whereEqualTo("objectId", currentUserId);
        thisUser.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    point = userList.get(0).getParseGeoPoint("location");
                } else {
                    Toast.makeText(getApplicationContext(), "Unable to retrieve location, please try again.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo("objectId", currentUserId);
        query.whereNotEqualTo("objectId", "2HDU94lVzB");
        query.whereWithinMiles("location", point, 8);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> userList, com.parse.ParseException e) {
                if (e == null) {
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getObjectId().toString().equals(currentUserId)) {
                        } else {
                            if (userList.get(i).get("facebookId").toString() != null) {
                                nearbyFacebookId.add(userList.get(i).get("facebookId").toString());
                            }
                            nearbyNames.add(userList.get(i).getUsername().toString());
                        }
                    }
                    for(picCount = 0; picCount<nearbyFacebookId.size(); picCount++) {
                        // set the profile picture using their Facebook ID
                        if (picCount == 0) {
                            pic1 = (ProfilePictureView) findViewById(R.id.myProfilePic1);
//                            new ProfilePictureView()
                            pic1.setVisibility(View.VISIBLE);
                            pic1.setProfileId(nearbyFacebookId.get(picCount).toString());
                            Log.d("facebook", nearbyFacebookId.get(picCount).toString());
                            pic1.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    showPopup(nearbyNames, 0);
//                                    openConversation(nearbyNames, 0);
                                }
                            });
                        }
                        if (picCount == 1) {
                            pic2 = (ProfilePictureView) findViewById(R.id.myProfilePic2);
                            pic2.setVisibility(View.VISIBLE);
                            pic2.setProfileId(nearbyFacebookId.get(picCount).toString());
                            Log.d("facebook", nearbyFacebookId.get(picCount).toString());
                            pic2.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    showPopup(nearbyNames, 1);
//                                    openConversation(nearbyNames, 1);
                                }
                            });
                        }
                        if (picCount == 2) {
                            pic3.setVisibility(View.VISIBLE);
                            pic3 = (ProfilePictureView) findViewById(R.id.myProfilePic3);
                            pic3.setProfileId(nearbyFacebookId.get(picCount).toString());
                            Log.d("facebook", nearbyFacebookId.get(picCount).toString());
                            pic3.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    showPopup(nearbyNames, 2);
//                                    openConversation(nearbyNames, 2);
                                }
                            });
                        }
                        if (picCount == 3) {
                            pic4.setVisibility(View.VISIBLE);
                            pic4 = (ProfilePictureView) findViewById(R.id.myProfilePic4);
                            pic4.setProfileId(nearbyFacebookId.get(picCount).toString());
                            Log.d("facebook", nearbyFacebookId.get(picCount).toString());
                            pic4.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    showPopup(nearbyNames, 3);
//                                    openConversation(nearbyNames, 3);
                                }
                            });
                        }
                    }
                    ParseUser user = ParseUser.getCurrentUser();
                    user.put("unread", true);
                    user.saveInBackground();
                }
            }
        });
    }

    public void showPopup(final ArrayList<String> names, final int pos)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(names.get(pos))
                .setMessage("Confirm to send a chat request.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getApplicationContext(), "Request sent to " + names.get(pos), Toast.LENGTH_SHORT).show();
                        ParseQuery<ParseUser> query = ParseUser.getQuery();
                        query.whereEqualTo("username", names.get(pos));
                        query.findInBackground(new FindCallback<ParseUser>() {
                            public void done(List<ParseUser> user, com.parse.ParseException e) {
                                if (e == null) {
                                } else {
                                    Toast.makeText(getApplicationContext(), "Error finding this user", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

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
                    Toast.makeText(getApplicationContext(), "Error finding this user", Toast.LENGTH_SHORT).show();
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
