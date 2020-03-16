package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.social_media_app.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.shobhitpuri.custombuttons.GoogleSignInButton;

import java.util.HashMap;
import java.util.Objects;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "";
    private static final int RC_SIGN_IN = 100;
    GoogleSignInClient googleSignInClient;

    EditText emailEditText, passwordEditText;
    TextView notHaveAccount, recoverPasswordText;
    Button loginButton;
    // GoogleSignIn googleSignIn;
    GoogleSignInButton googleSignInButton;

    // Declare an instance of Firebase Auth
    private FirebaseAuth firebaseAuth;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //  Action and its title
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Login Account");

        //  enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        // init id
        emailEditText = findViewById(R.id.emailText);
        passwordEditText = findViewById(R.id.passwordText);
        notHaveAccount = findViewById(R.id.notHaveAccountText);
        recoverPasswordText = findViewById(R.id.recoverPasswordText);
        loginButton = findViewById(R.id.loginButtonSign);
        googleSignInButton = findViewById(R.id.googleLoginButton);

        // before firebase auth
        // configure google sign in
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        // Initialize firebase Auth
        // in the onCreate() method, initialize the firebase Auth instance
        firebaseAuth = FirebaseAuth.getInstance();

        // login button click
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // input data
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString().trim();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    // invalid email patters set error
                    emailEditText.setError("Invalid Email");
                    emailEditText.setFocusable(true);
                } else {
                    // valid email pattern
                    loginUser(email, password);
                }

            }
        });

        // not have account text view click
        notHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });

        // recover password text view click
        recoverPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });

        // handle google login button click
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // begin google login process
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        // init progress dialog
        progressDialog = new ProgressDialog(this);

    }

    private void showRecoverPasswordDialog() {

        // alertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");

        // set linear layout
        LinearLayout linearLayout = new LinearLayout(this);

        // view to set in dialog
        final EditText emailEditText = new EditText(this);
        emailEditText.setHint("Email");
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // sets the min width of a EditView to fit a text of a 'M' letters regardless of the actual text extension and text size.
        emailEditText.setMinEms(16);

        linearLayout.addView(emailEditText);
        linearLayout.setPadding(10, 10, 10, 10);

        builder.setView(linearLayout);

        // button recover
        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // input email
                String email = emailEditText.getText().toString().trim();
                beginRecovery(email);
            }
        });

        // button cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dismiss dialog
                dialog.dismiss();
            }
        });

        // show dialog
        builder.create().show();

    }

    private void beginRecovery(String email) {
        // show progress dialog
        progressDialog.setMessage("Sending email...");
        progressDialog.show();
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed...", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                // get and show proper error message
                Toast.makeText(LoginActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String email, String password) {
        // show progress dialog
        progressDialog.setMessage("Logging in...");
        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // dismiss progress dialog
                            progressDialog.dismiss();

                            // Sign in success, update UI with the signed-in user'AdapterPosts information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();

                        } else {
                            // dismiss progress dialog
                            progressDialog.dismiss();

                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // dismiss progress dialog
                progressDialog.dismiss();
                // error, get and show error message
                Toast.makeText(LoginActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // go previous page
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the intent from GoogleSignInApi.getSignIntent(..);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google sign in was successful,authenticate with firebase
                GoogleSignInAccount googleSignInAccount = task.getResult(ApiException.class);
                assert googleSignInAccount != null;
                firebaseAuthWithGoogle(googleSignInAccount);

            } catch (ApiException e) {
                // Google sign in failed,update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        // Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user'AdapterPosts information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            // if user is signing in first time then get and show user into from google account
                            if (Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getAdditionalUserInfo()).isNewUser()){

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
                                hashMap.put("onlineStatus", "online");
                                hashMap.put("typingTo", "noOne");
                                hashMap.put("phone", ""); // will add later
                                hashMap.put("image", ""); // will add later
                                hashMap.put("cover", ""); // will add later

                                // firebase database instance
                                FirebaseDatabase database = FirebaseDatabase.getInstance();

                                // path to store user data named "AdapterUsers"
                                DatabaseReference reference = database.getReference("AdapterUsers");

                                // put data within hash map in database
                                reference.child(uid).setValue(hashMap);
                            }


                            Toast.makeText(LoginActivity.this, "" + user.getEmail(), Toast.LENGTH_SHORT).show();
                            // go to profile activity after logged in
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();

                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // get and show error message
                Toast.makeText(LoginActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
