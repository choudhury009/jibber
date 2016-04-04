package com.choudhurylad.jibber;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.messaging.Message;
import com.sinch.android.rtc.messaging.MessageClient;
import com.sinch.android.rtc.messaging.MessageClientListener;
import com.sinch.android.rtc.messaging.MessageDeliveryInfo;
import com.sinch.android.rtc.messaging.MessageFailureInfo;
import com.sinch.android.rtc.messaging.WritableMessage;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MessagingActivity extends AppCompatActivity {

    private String recipientId;
    private String username;
    private String messageBody;
    private String currentUserId;
    private EditText messageBodyField;
    private MessageService.MessageServiceInterface messageService;
    private MessageAdapter messageAdapter;
//    private ListUsersActivity listUsersActivity;
    private ListView messagesList;
    private ServiceConnection serviceConnection = new MyServiceConnection();
    private MessageClientListener messageClientListener = new MyMessageClientListener();
    public static ArrayList<String> recipNames = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messaging);

        bindService(new Intent(this, MessageService.class), serviceConnection, BIND_AUTO_CREATE);

        Intent intent = getIntent();
        recipientId = intent.getStringExtra("RECIPIENT_ID");
        currentUserId = ParseUser.getCurrentUser().getObjectId();
        username = intent.getStringExtra("MESSAGE_USERNAME");
//
        Toolbar myChildToolbar = (Toolbar) findViewById(R.id.my_child_toolbar);
        setSupportActionBar(myChildToolbar);
        setTitle(username);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        messagesList = (ListView) findViewById(R.id.listMessages);
        messageAdapter = new MessageAdapter(this);
        messagesList.setAdapter(messageAdapter);
        // special feature that removes messages once the conversation is closed!!
         populateMessageHistory();

        messageBodyField = (EditText) findViewById(R.id.messageBodyField);

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    public static ArrayList recipNames() {
        return recipNames;
    }
    //get previous messages from parse & display
    private void populateMessageHistory() {
        String[] userIds = {currentUserId, recipientId};
        ParseQuery<ParseObject> query = ParseQuery.getQuery("ParseMessage");
        query.whereContainedIn("senderId", Arrays.asList(userIds));
        query.whereContainedIn("recipientId", Arrays.asList(userIds));
        query.orderByAscending("createdAt");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messageList, com.parse.ParseException e) {
                if (e == null) {
                    for (int i = 0; i < messageList.size(); i++) {
                        WritableMessage message = new WritableMessage(messageList.get(i).get("recipientId").toString(), messageList.get(i).get("messageText").toString());
                            if (messageList.get(i).get("senderId").toString().equals(currentUserId)) {
                                messageAdapter.addMessage(message, MessageAdapter.DIRECTION_INCOMING);
                            }else {
                                messageAdapter.addMessage(message, MessageAdapter.DIRECTION_OUTGOING);
                            }
                    }
                }
            }
        });
    }

    private void sendMessage() {
        messageBody = messageBodyField.getText().toString();
        if (messageBody.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_LONG).show();
            return;
        }

        messageService.sendMessage(recipientId, messageBody);
        messageBodyField.setText("");
        ParseQuery userQuery = ParseUser.getQuery();
        userQuery.whereEqualTo("objectId", recipientId);

        ParseQuery pushQuery = ParseInstallation.getQuery();
        pushQuery.whereMatchesQuery("user", userQuery);

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject();
            jsonObject.put("title", "Jibber");
            jsonObject.put("alert", ParseUser.getCurrentUser().getUsername() + " sent you a message");

            ParsePush push = new ParsePush();
            push.setQuery(pushQuery);
            push.setData(jsonObject);
            push.sendInBackground();
//            recipNames.add(ParseUser.getCurrentUser().getUsername());
        }catch(Exception e){
            Log.v("Parse", "An exception has occured!");
        }
    }

    @Override
    public void onDestroy() {
        messageService.removeMessageClientListener(messageClientListener);
        unbindService(serviceConnection);
        super.onDestroy();
    }

    private class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            messageService = (MessageService.MessageServiceInterface) iBinder;
            messageService.addMessageClientListener(messageClientListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            messageService = null;
        }
    }

    private class MyMessageClientListener implements MessageClientListener {
        @Override
        public void onMessageFailed(MessageClient client, Message message,
                                    MessageFailureInfo failureInfo) {
            Toast.makeText(MessagingActivity.this, "Message failed to send.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onIncomingMessage(MessageClient client, final Message message) {
            if (message.getSenderId().equals(recipientId)) {
                final WritableMessage writableMessage = new WritableMessage(message.getRecipientIds().get(0), message.getTextBody());
    
                //only add message to parse database if it doesn't already exist there
//                Toast.makeText(getApplicationContext(), "recip " + recipientId, Toast.LENGTH_LONG).show();
//                Toast.makeText(getApplicationContext(), "current " +currentUserId, Toast.LENGTH_LONG).show();

                ParseQuery<ParseObject> query = ParseQuery.getQuery("ParseMessage");
                query.whereEqualTo("sinchId", message.getMessageId());
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> messageList, com.parse.ParseException e) {
                        if (e == null) {
                            if (messageList.size() == 0) {
                                ParseObject parseMessage = new ParseObject("ParseMessage");
                                parseMessage.put("senderId", currentUserId);
                                parseMessage.put("recipientId", recipientId);
                                parseMessage.put("messageText", writableMessage.getTextBody());
                                parseMessage.put("sinchId", message.getMessageId());
//                                parseMessage.put("unread", true);
                                parseMessage.saveInBackground();

                                messageAdapter.addMessage(writableMessage, MessageAdapter.DIRECTION_INCOMING);
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void onMessageSent(MessageClient client, Message message, String recipientId) {


          final WritableMessage writableMessage = new WritableMessage(message.getRecipientIds().get(0), message.getTextBody());
            messageAdapter.addMessage(writableMessage, MessageAdapter.DIRECTION_OUTGOING);
        }

        @Override
        public void onMessageDelivered(MessageClient client, MessageDeliveryInfo deliveryInfo) {

        }

        @Override
        public void onShouldSendPushData(MessageClient client, Message message, List<PushPair> pushPairs) {}
    }
}
