package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;


import com.example.social_media_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends AppCompatActivity {

    SwitchCompat postSwitchCompat;
    // user shared preference to save the state of switch
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor; // to edit value of shared preference

    // constants for topic
    private static final String TOPIC_POST_NOTIFICATION = "POST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Settings");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // init views
        postSwitchCompat = findViewById(R.id.postSwitch);
        // init sharedPreferences
        sharedPreferences = getSharedPreferences("Notification_SP", MODE_PRIVATE);
        boolean isPostEnabled = sharedPreferences.getBoolean("" + TOPIC_POST_NOTIFICATION, false);

        // if enabled check switch ,otherwise uncheck switch - by default unchecked/false
        if (isPostEnabled) {
            postSwitchCompat.setChecked(true);
        } else {
            postSwitchCompat.setChecked(false);
        }


        // implement switch change listener
        postSwitchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // edit switch state
                editor = sharedPreferences.edit();
                editor.putBoolean("" + TOPIC_POST_NOTIFICATION, isChecked);
                editor.apply();
                if (isChecked) {
                    subscribePostNotification(); // call to subscribe
                } else {
                    unsubscribePostNotification(); // call to unsubscribe
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    private void unsubscribePostNotification() {
        // unsubscribe to a topic (POST) to disable it's notification
        FirebaseMessaging.getInstance().unsubscribeFromTopic("" + TOPIC_POST_NOTIFICATION)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String message = "You will not receive post notification";
                        if (!task.isSuccessful()) {
                            message = "UnSubscription failed";
                        }
                        Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void subscribePostNotification() {
        // subscribe to a topic (POST) to disable it's notification
        FirebaseMessaging.getInstance().subscribeToTopic("" + TOPIC_POST_NOTIFICATION)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String message = "You will receive post notification";
                        if (!task.isSuccessful()) {
                            message = "Subscription failed";
                        }
                        Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
