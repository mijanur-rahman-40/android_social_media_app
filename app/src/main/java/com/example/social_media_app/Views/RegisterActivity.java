package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.social_media_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "";
    EditText emailEditText, passwordEditText;
    Button registerButton;

    TextView haveAccount;

    ProgressDialog progressDialog;

    // declare firebase auth
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //  Action and its title
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Create Account");

        //  enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);


        //  init

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.addNewUser);
        haveAccount = findViewById(R.id.haveAccountText);

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");

        //  handle register button click
        registerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // get input value
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                // validate
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    // set error and focus to email edit text
                    emailEditText.setError("Invalid Email");
                    emailEditText.setFocusable(true);
                } else if (password.length() < 6) {
                    passwordEditText.setError("Password length at least 6 characters");
                    passwordEditText.setFocusable(true);
                } else {
                    registerUser(email, password);
                }
            }
        });

        // handle login text view click listener
        haveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser(String email, String password) {
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            progressDialog.dismiss();
                            // Sign in success, dismiss dialog and start register activity
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            // get user email and uid from auth
                            assert user != null;
                            String email = user.getEmail();
                            String uid = user.getUid();

                            // when user is registered store user info in firebase realtime database too
                            // using hashMap
                            HashMap<Object, String> hashMap = new HashMap<>();

                            // put data into hash map
                            hashMap.put("email", email);
                            hashMap.put("uid", uid);
                            hashMap.put("name", "");  // will add later
                            hashMap.put("onlineStatus", "online");  // will add later
                            hashMap.put("typingTo", "noOne");  // will add later
                            hashMap.put("phone", ""); // will add later
                            hashMap.put("image", ""); // will add later
                            hashMap.put("cover", ""); // will add later

                            // firebase database instance
                            FirebaseDatabase database = FirebaseDatabase.getInstance();

                            // path to store user data named "Users"
                            DatabaseReference databaseReference = database.getReference("Users");

                            // put data within hash map in database
                            databaseReference.child(uid).setValue(hashMap);

                            Toast.makeText(RegisterActivity.this, "Registered...\n" + user.getEmail(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            progressDialog.dismiss();
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());

                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }


                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                // error , dismiss progress dialog and get and show the error message.
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); //  go previous activity
        return super.onSupportNavigateUp();
    }
}