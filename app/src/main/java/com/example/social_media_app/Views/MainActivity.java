package com.example.social_media_app.Views;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.social_media_app.R;

public class MainActivity extends AppCompatActivity {

    //  Views
    Button loginButton, registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  int views
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);

        //  handle register button click
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  start Register Activity
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });

        // handle login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  start Login Activity
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });
    }
}