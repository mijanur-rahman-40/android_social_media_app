package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


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

import java.util.HashMap;

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

    String hisUID, myUID;

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

        /* On clicking user from users list we have passed that user's UID using intent
         * So get that uid here to get the profile picture,name and start char with that user
         * */
        Intent intent = getIntent();
        hisUID = intent.getStringExtra("hisUID");

        // firebase init
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDatabaseReference = firebaseDatabase.getReference("Users");

        // search user to get that user's info
        Query userQuery = usersDatabaseReference.orderByChild("uid").equalTo(hisUID);

        // get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // check until required info os received
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    // get data
                    String name = "" + ds.child("name").getValue();
                    String image = "" + ds.child("image").getValue();

                    // set data
                    nameText.setText(name);

                    try {
                        // image received, set it to image view in toolbar
                        Picasso.get().load(image).placeholder(R.drawable.ic_default_profile).into(profileImage);

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
            }
        });
    }

    private void sendMessage(String message) {
        /* "Chats node will be created that will contain all chats"
        * Whenever user sends message it will create new child in "Chats" node and that child will contain
        * the following key values
        * sender : UID of sender
        * receiver : UID if receiver
        * message: the actual message
        * */

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender",myUID);
        hashMap.put("receiver",hisUID);
        hashMap.put("message",message);

        databaseReference.child("Chats").push().setValue(hashMap);

        // reset edit text after sending message
        messageEditText.setText("");
    }

    private void checkUserStatus() {
        // get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            // set email of logged in user
            // emailText.setText(user.getEmail());
            myUID = user.getUid(); // currently signed in user's uid
        } else {
            // user not sign in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    // inflate menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // hide search view, as we do not need here
        menu.findItem(R.id.actionSearch).setVisible(false);
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
