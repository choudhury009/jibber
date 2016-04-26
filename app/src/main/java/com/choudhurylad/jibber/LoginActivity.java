package com.choudhurylad.jibber;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

//import com.facebook.FacebookSdk;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.parse.LogInCallback;
import com.parse.ParseException;
//import com.parse.ParseFacebookUtils;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends Activity {

    private Intent intent;
    private View fbLoginButton;
    private String userEmail;
    private String fbName;
    private String fbSurname;
    private String fbId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        FacebookSdk.sdkInitialize(getApplicationContext());
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        class RegisterGcmTask extends AsyncTask<Void, Void, String> {
            String msg = "";
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    msg = gcm.register("896369439262");
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                Log.d("msg",msg);
                return msg;
            }
            @Override
            protected void onPostExecute(String msg) {
                // msg is deviceToken
                intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("regId", msg);
                startActivity(intent);
            }
        }

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            (new RegisterGcmTask()).execute();
        }

        fbLoginButton = (View) findViewById(R.id.fb_login_button);
        fbLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> permissions = Arrays.asList("public_profile", "email");
                ParseFacebookUtils.logInWithReadPermissionsInBackground(LoginActivity.this, permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException err) {
                        if (user == null) {
                            Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                        } else if (user.isNew()) {
                            Log.d("MyApp", "User signed up and logged in through Facebook!");
                            getFacebookUserDetails(false, user, err);
                        } else {
                            Log.d("MyApp", "User logged in through Facebook!");
                            getFacebookUserDetails(false, user, err);
                        }
                    }
                });
            }
        });
    }
    public void getFacebookUserDetails(final boolean firstTime, final ParseUser user, final ParseException error) {

        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        // Application code
                        try {
                            fbName =  object.getString("first_name");
                            fbSurname = object.getString("last_name");
                            userEmail = object.getString("email");
                            fbId = object.getString("id");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //User logged in with facebook for the first time
                        if (firstTime) {
                            if (!ParseFacebookUtils.isLinked(user)) {
                            } else {
                                user.setUsername(fbName + " " + fbSurname);
                                user.setEmail(userEmail);
                                user.put("facebookId", fbId);
                                user.saveInBackground();
                            }
                        } else {
                            if (!ParseFacebookUtils.isLinked(user)) {
                                List<String> permissions = Arrays.asList("public_profile", "email");
                                ParseFacebookUtils.linkWithReadPermissionsInBackground(user, LoginActivity.this, permissions, new SaveCallback() {
                                    @Override
                                    public void done(ParseException ex) {
                                        if (ParseFacebookUtils.isLinked(user)) {
                                            user.setUsername(fbName + " " + fbSurname);
                                            user.setEmail(userEmail);
                                            user.put("facebookId", fbId);
                                            user.saveInBackground();
                                            intent = new Intent(getApplicationContext(), MainActivity.class);
                                            startActivity(intent);
                                        }
                                    }
                                });
                            } else {
                                user.setUsername(fbName + " " + fbSurname);
                                user.setEmail(userEmail);
                                user.put("facebookId", fbId);
                                user.saveInBackground();
                                intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                            }
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,email,first_name,last_name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
