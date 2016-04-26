package com.choudhurylad.jibber;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.parse.Parse;
//import com.parse.ParseFacebookUtils;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.PushService;
import com.parse.SaveCallback;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

//        Parse.initialize(new Parse.Configuration.Builder(this)
//                .applicationId("YOUR_APP_ID")
//                .server("http://YOUR_PARSE_SERVER:1337/parse")
//        .build()
//        );

        Parse.initialize(this, "aDX5iBp3f52JMumnQOUl9aDS1LQ7FzmWBx8hKiLh", "W6zrLHvIv9E9ZNTiOu8JTR3IHrkw3bZWO0LtQCAP");
//        ParseFacebookUtils.initialize("1645380625723200");
        ParseFacebookUtils.initialize(this);
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParsePush.subscribeInBackground("Jibber", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                Log.e("myappfile", "Successfully subscribed to Parse!");
            }
        });
    }
}
