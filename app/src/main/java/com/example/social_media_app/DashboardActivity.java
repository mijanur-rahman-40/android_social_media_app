package com.example.social_media_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {

    // firebase auth
    FirebaseAuth firebaseAuth;

    ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //  Action and its title
        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Profile");

        // firebase init
        firebaseAuth = FirebaseAuth.getInstance();

        // bottom navigation
        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        // home fragment transaction (default on start)
        actionBar.setTitle("Home"); // change action bar title
        HomeFragment homeFragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content, homeFragment, "");
        fragmentTransaction.commit();

    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            // handle fragment transaction
            switch (item.getItemId()) {
                case R.id.nav_home:
                    // home fragment transaction
                    actionBar.setTitle("Home"); // change action bar title
                    HomeFragment homeFragment = new HomeFragment();
                    FragmentTransaction fragmentTransaction_1 = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction_1.replace(R.id.content, homeFragment, "");
                    fragmentTransaction_1.commit();
                    return true;

                case R.id.nav_profile:
                    // profile fragment transaction
                    actionBar.setTitle("Profile"); // change action bar title
                    ProfileFragment profileFragment = new ProfileFragment();
                    FragmentTransaction fragmentTransaction_2 = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction_2.replace(R.id.content, profileFragment, "");
                    fragmentTransaction_2.commit();
                    return true;

                case R.id.nav_users:
                    // users fragment transaction
                    actionBar.setTitle("Users"); // change action bar title
                    UsersFragment usersFragment = new UsersFragment();
                    FragmentTransaction fragmentTransaction_3 = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction_3.replace(R.id.content, usersFragment, "");
                    fragmentTransaction_3.commit();
                    return true;

            }
            return false;
        }
    };

    private void checkUserStatus() {
        // get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            // set email of logged in user
            // emailText.setText(user.getEmail());
        } else {
            // user not sign in, go to main activity
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        // check on start of app
        checkUserStatus();
        super.onStart();
    }

    // inflate option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate menu_main
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle menu item click

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
