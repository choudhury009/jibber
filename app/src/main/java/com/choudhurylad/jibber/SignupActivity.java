package com.choudhurylad.jibber;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SignupActivity extends Activity {

    private Button signUpButton;
    private Button loginButton;
    private EditText usernameField;
    private EditText passwordField;
    private EditText emailField;
    private String username;
    private String password;
    private String email;
    private Intent intent;
    private Intent serviceIntent;
    private ProgressDialog progressDialog;
    private View fbLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

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

        loginButton = (Button) findViewById(R.id.signupLoginButton);
        signUpButton = (Button) findViewById(R.id.signUpButton);
        usernameField = (EditText) findViewById(R.id.signupUsername);
        passwordField = (EditText) findViewById(R.id.signupPassword);
        emailField = (EditText) findViewById(R.id.signupEmail);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                username = usernameField.getText().toString();
                password = passwordField.getText().toString();
                email = emailField.getText().toString();

                // check if username and password fields are not empty
                if (username.length() < 4 || password.length() < 4) {
                    Toast.makeText(getApplicationContext(),
                            "Please ensure username and password are at least 4 characters.",
                            Toast.LENGTH_LONG).show();
                } else {
                    ParseUser user = new ParseUser();
                    user.setUsername(username);
                    user.setPassword(password);
                    user.setEmail(email);

                    user.signUpInBackground(new SignUpCallback() {
                        public void done(com.parse.ParseException e) {
                            if (e == null) {
                                (new RegisterGcmTask()).execute();
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "There was an error signing up."
                                        , Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
        fbLoginButton = (View) findViewById(R.id.fb_signup_button);
        fbLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> permissions = Arrays.asList("basic_info", "user_about_me",
                        "user_relationships", "user_birthday", "user_location", "email");
                ParseFacebookUtils.logInWithReadPermissionsInBackground(SignupActivity.this, permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException err) {
                        if (user == null) {
                            Log.d("MyApp", "Uh oh. The user cancelled the Facebook login.");
                        } else if (user.isNew()) {
                            Log.d("MyApp", "User signed up and logged in through Facebook!");
                        } else {
                            Log.d("MyApp", "User logged in through Facebook!");
                            (new RegisterGcmTask()).execute();
                        }
                    }
                });
            }
        });
        CallbackManager callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                    }

                    @Override
                    public void onCancel() {
                        // App code
                        ParseUser.logOut();
                        intent = new Intent(getApplicationContext(), SignupActivity.class);
                        startActivity(intent);

                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });
    }

}
