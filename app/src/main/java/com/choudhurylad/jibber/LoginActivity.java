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

    private Button signUpButton;
    private Button loginButton;
    private EditText usernameField;
    private EditText passwordField;
    private String username;
    private String password;
    private Intent intent;
    private Intent serviceIntent;
    private ProgressDialog progressDialog;
    private View fbLoginButton;
    private String userEmail;
    private String fbName;
    private String fbSurname;
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
                serviceIntent = new Intent(getApplicationContext(), MessageService.class);
                intent.putExtra("regId", msg);
                startActivity(intent);
                startService(serviceIntent);
            }
        }

        intent = new Intent(getApplicationContext(), MainActivity.class);
        serviceIntent = new Intent(getApplicationContext(), MessageService.class);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            (new RegisterGcmTask()).execute();
        }

//        loginButton = (Button) findViewById(R.id.loginButton);
//        signUpButton = (Button) findViewById(R.id.signupButton);
//        usernameField = (EditText) findViewById(R.id.loginUsername);
//        passwordField = (EditText) findViewById(R.id.loginPassword);

//        loginButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                username = usernameField.getText().toString();
//                password = passwordField.getText().toString();
//                if (username.length() < 4) {
//                    Toast.makeText(getApplicationContext(), "Your username is too short", Toast.LENGTH_LONG).show();
//                } else if (password.length() < 4) {
//                    Toast.makeText(getApplicationContext(), "Your password is too short", Toast.LENGTH_LONG).show();
//                } else {
//                    ParseUser.logInInBackground(username, password, new LogInCallback() {
//                        public void done(ParseUser user, com.parse.ParseException e) {
//                            if (user != null) {
//                                (new RegisterGcmTask()).execute();
//                            } else {
//                                Toast.makeText(getApplicationContext(), "Wrong username/password", Toast.LENGTH_LONG).show();
//                            }
//                        }
//                    });
//                }
//            }
//        });
//
//        signUpButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                intent = new Intent(getApplicationContext(), SignupActivity.class);
//                startActivity(intent);
//
////                username = usernameField.getText().toString();
////                password = passwordField.getText().toString();
////
////                // check if username and password fields are not empty
////                if (username.length() < 4 || password.length() < 4) {
////                    Toast.makeText(getApplicationContext(),
////                            "Please ensure username and password are at least 4 characters.",
////                            Toast.LENGTH_LONG).show();
////                } else {
////                    ParseUser user = new ParseUser();
////                    user.setUsername(username);
////                    user.setPassword(password);
////
////                    user.signUpInBackground(new SignUpCallback() {
////                        public void done(com.parse.ParseException e) {
////                            if (e == null) {
////                                (new RegisterGcmTask()).execute();
////                            } else {
////                                Toast.makeText(getApplicationContext(),
////                                        "There was an error signing up."
////                                        , Toast.LENGTH_LONG).show();
////                            }
////                        }
////                    });
////                }
//            }
//        });
        fbLoginButton = (View) findViewById(R.id.fb_login_button);
        fbLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> permissions = Arrays.asList("public_profile", "user_friends", "user_about_me",
                        "user_relationships", "user_birthday", "user_location", "email");
                ParseFacebookUtils.logInWithReadPermissionsInBackground(LoginActivity.this, permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException err) {
                        if (user == null) {
                            Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                        } else if (user.isNew()) {
                            Log.d("MyApp", "User signed up and logged in through Facebook!");
                        } else {
                            Log.d("MyApp", "User logged in through Facebook!");
//                            (new RegisterGcmTask()).execute();
                            getFacebookUserDetails(false, user, err);
//                            CallbackManager callbackManager = CallbackManager.Factory.create();
//
//                            LoginManager.getInstance().registerCallback(callbackManager,
//                                    new FacebookCallback<LoginResult>() {
//                                        @Override
//                                        public void onSuccess(LoginResult loginResult) {
//                                            // App code
//                                            GraphRequest request = GraphRequest.newMeRequest(
//                                                    loginResult.getAccessToken(),
//                                                    new GraphRequest.GraphJSONObjectCallback() {
//                                                        @Override
//                                                        public void onCompleted(JSONObject object, GraphResponse response) {
//                                                            Log.v("LoginActivity", response.toString());
//
//                                                            // Application code
//                                                            try {
//                                                                String email = object.getString("email");
//                                                            } catch (JSONException e) {
//                                                                Log.e("MYAPP", "unexpected JSON exception", e);
//                                                            }
//                                                            try {
//                                                                String birthday = object.getString("birthday"); // 01/31/1980 format
//                                                            } catch (JSONException e) {
//                                                                Log.e("MYAPP", "unexpected JSON exception", e);
//                                                            }
//                                                            Toast.makeText(getApplicationContext(), "here", Toast.LENGTH_LONG).show();
//                                                        }
//                                                    });
//                                            Bundle parameters = new Bundle();
//                                            parameters.putString("fields", "id,name,email,gender, birthday");
//                                            request.setParameters(parameters);
//                                            request.executeAsync();
//
//                                        }
//
//                                        @Override
//                                        public void onCancel() {
//                                            // App code
//                                            ParseUser.logOut();
//                                            intent = new Intent(getApplicationContext(), LoginActivity.class);
//                                            startActivity(intent);
//                                            Toast.makeText(getApplicationContext(), "cancelled", Toast.LENGTH_LONG).show();
//
//                                        }
//
//                                        @Override
//                                        public void onError(FacebookException exception) {
//                                            // App code
//                                            Toast.makeText(getApplicationContext(), exception.toString(), Toast.LENGTH_LONG).show();
//                                        }
//                                    });
                        }
                    }
                });
            }
        });
    }
    public void getFacebookUserDetails(final boolean firstTime, final ParseUser user, final ParseException error) {

        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        // Application code
                        try {
                            fbName =  object.getString("first_name");
                            fbSurname = object.getString("last_name");
                            userEmail = object.getString("email");


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //User logged in with facebook for the first time
                        if (firstTime) {

                            if (!ParseFacebookUtils.isLinked(user)) {

                            } else {
                                user.setUsername(fbName + " " + fbSurname);
                                user.setEmail(userEmail);
                                user.saveInBackground();
                            }

                        } else {

                            if (!ParseFacebookUtils.isLinked(user)) {
                                List<String> permissions = Arrays.asList("basic_info", "user_about_me",
                                        "user_relationships", "user_birthday", "user_location", "email");
                                ParseFacebookUtils.linkWithReadPermissionsInBackground(user, LoginActivity.this, permissions, new SaveCallback() {
                                    @Override
                                    public void done(ParseException ex) {
                                        if (ParseFacebookUtils.isLinked(user)) {

                                            user.setUsername(fbName + " " + fbSurname);
                                            user.setEmail(userEmail);
                                            user.saveInBackground();

                                            intent = new Intent(getApplicationContext(), MainActivity.class);
                                            serviceIntent = new Intent(getApplicationContext(), MessageService.class);
                                            startActivity(intent);
                                            startService(serviceIntent);
                                        }
                                    }
                                });
                            } else {

                                user.setUsername(fbName + " " + fbSurname);
                                user.setEmail(userEmail);
                                user.saveInBackground();

                                intent = new Intent(getApplicationContext(), MainActivity.class);
                                serviceIntent = new Intent(getApplicationContext(), MessageService.class);
                                startActivity(intent);
                                startService(serviceIntent);
                            }
                        }

                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "email,first_name,last_name");
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
        stopService(new Intent(this, MessageService.class));
        super.onDestroy();
    }
}
