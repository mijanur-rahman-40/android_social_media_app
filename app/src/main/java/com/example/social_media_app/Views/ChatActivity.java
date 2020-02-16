package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.social_media_app.Adapters.AdapterChat;
import com.example.social_media_app.Models.ModelChat;
import com.example.social_media_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class ChatActivity extends AppCompatActivity {

    // views from xml
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileImage;
    TextView nameText, statusText;
    EditText messageEditText;
    ImageButton sendButton;

    // firebase auth
    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDatabaseReference;

    // for checking if user has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userReferenceForSeen;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String hisUID, myUID;

   // APIService apiService;
    boolean notify = false;

    // image string
    String hisImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");


        recyclerView = findViewById(R.id.chatRecyclerView);
        profileImage = findViewById(R.id.profileImageView);
        nameText = findViewById(R.id.userNameText);
        statusText = findViewById(R.id.userStatusText);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        // Layout (LinearLayout) for recyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        // recycler view properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        // create api service
        // apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);


        /* On clicking user from users list we have passed that user'AdapterPosts UID using intent
         * So get that uid here to get the profile picture,name and start char with that user
         * */
        Intent intent = getIntent();
        hisUID = intent.getStringExtra("hisUID");

        // firebase init
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDatabaseReference = firebaseDatabase.getReference("Users");

        // search user to get that user'AdapterPosts info
        Query userQuery = usersDatabaseReference.orderByChild("uid").equalTo(hisUID);

        // get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // check until required info os received
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    // get data
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();
                    String typingStatus = "" + ds.child("typingTo").getValue();

                    // check typing status
                    if (typingStatus.equals(myUID)) {
                        String typing = "typing...";
                        statusText.setText(typing);
                    } else {
                        // get value of online status
                        String onlineStatus = "" + ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")) {
                            statusText.setText(onlineStatus);
                        } else {
                            // convert timestamp to proper time date
                            // convert time stamp to dd/mm/yyyy hh:mm am/pm
                            Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                            if (!TextUtils.isEmpty(onlineStatus)) {
                                calendar.setTimeInMillis(Long.parseLong(onlineStatus));
                            }
                            String onlineStatusTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
                            String temp = "Last seen at : " + onlineStatusTime;
                            statusText.setText(temp);
                        }
                    }

                    // set data
                    nameText.setText(name);

                    try {
                        // image received, set it to image view in toolbar
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_profile).into(profileImage);

                    } catch (Exception e) {
                        // there is exception getting picture, so set default picture
                        Picasso.get().load(R.drawable.ic_default_profile).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // click button to send message
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;
                // get text from edit text
                String message = messageEditText.getText().toString().trim();

                // check if text is empty or not
                if (TextUtils.isEmpty(message)) {
                    // text empty
                    Toast.makeText(ChatActivity.this, "Can not send empty message...", Toast.LENGTH_SHORT).show();
                } else {
                    // text not empty
                    sendMessage(message);
                }
                // reset edit text after sending message
                messageEditText.setText("");
            }
        });

        // check edit text change listener
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0) {
                    checkTypingStatus("noOne");
                } else {
                    checkTypingStatus(hisUID); //UID of receiver
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        readMessages();

        seenMessage();
    }


    private void readMessages() {
        chatList = new ArrayList<>();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    ModelChat modelChat = ds.getValue(ModelChat.class);

                    assert modelChat != null;
                    if (modelChat.getReceiver().equals(hisUID) && modelChat.getSender().equals(myUID) || modelChat.getReceiver().equals(myUID) && modelChat.getSender().equals(hisUID)) {
                        chatList.add(modelChat);
                    }

                    // adapter
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();

                    // set adapter to recycler view
                    recyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void seenMessage() {
        userReferenceForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userReferenceForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    ModelChat modelChat = ds.getValue(ModelChat.class);

                    assert modelChat != null;
                    if (modelChat.getReceiver().equals(myUID) && modelChat.getSender().equals(hisUID)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(final String message) {

        /* "Chats node will be created that will contain all chats"
         * Whenever user sends message it will create new child in "Chats" node and that child will contain
         * the following key values
         * sender : UID of sender
         * receiver : UID if receiver
         * message: the actual message
         * */

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timeStamp = String.valueOf(System.currentTimeMillis());
        //String timeStamp = "time";


        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUID);
        hashMap.put("receiver", hisUID);
        hashMap.put("message", message);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("isSeen", false);

        databaseReference.child("Chats").push().setValue(hashMap);



       /* DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(myUID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUser user = dataSnapshot.getValue(ModelUser.class);
                if (notify) {
                    // sendNotification(hisUID, user.getName(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });*/
    }

   /* private void sendNotification(final String hisUID, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUID);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);

                    Data data = new Data(myUID, name + ":" + message, "New Message", hisUID, R.drawable.ic_default_image);

                    Sender sender = new Sender(data, token.getToken());
                    apiService.sendNotification(sender)
                            .enqueue(new Callback<Response>() {
                                @Override
                                public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                                    Toast.makeText(ChatActivity.this, "" + response.message(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Call<Response> call, Throwable t) {

                                }
                            });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }*/

    private void checkUserStatus() {
        // get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {

            // user is signed in stay here
            // set email of logged in user
            // emailText.setText(user.getEmail());
            myUID = user.getUid(); // currently signed in user'AdapterPosts uid
        } else {
            // user not sign in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(myUID);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        // update value of onlineStatus of current user
        databaseReference.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(myUID);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        // update value of typingStatus of current user
        databaseReference.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        // set online
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // get timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        // not offline with last seen time stamp
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userReferenceForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        // set online
        checkOnlineStatus("online");
        super.onResume();
    }

    // inflate menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // hide search view, add post, as we do not need here
        menu.findItem(R.id.actionSearch).setVisible(false);
        menu.findItem(R.id.actionAddPost).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    // select menu item
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // get item id
        int id = item.getItemId();
        if (id == R.id.actionLogout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
