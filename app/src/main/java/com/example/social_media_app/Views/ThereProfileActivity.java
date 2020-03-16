package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.social_media_app.Adapters.AdapterPosts;
import com.example.social_media_app.Models.ModelPost;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ThereProfileActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    // views from paths
    private ImageView avatarImageView, coverImageView;
    private TextView nameText, emailText, phoneText;

    private RecyclerView postsRecyclerView;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Profile");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);


        // init views
        avatarImageView = findViewById(R.id.avatarImage);
        coverImageView = findViewById(R.id.coverImage);

        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        phoneText = findViewById(R.id.phoneText);

        // init views
        postsRecyclerView = findViewById(R.id.recyclerViewPosts);

        firebaseAuth = FirebaseAuth.getInstance();

        // get uid of clicked user to retrieve his posts
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // checks until required data got
                for (DataSnapshot data_snapshot : dataSnapshot.getChildren()) {

                    // get data
                    String name = "" + data_snapshot.child("name").getValue();
                    String email = "" + data_snapshot.child("email").getValue();
                    String phone = "" + data_snapshot.child("phone").getValue();
                    String image = "" + data_snapshot.child("image").getValue();
                    String cover = (String) data_snapshot.child("cover").getValue();

                    // set data
                    nameText.setText(name);
                    emailText.setText(email);
                    phoneText.setText(phone);

                    // for profile photo
                    try {
                        // if image is received then set
                        Picasso.get().load(image).into(avatarImageView);
                    } catch (Exception e) {
                        // if there is any exception while setting image then set default
                        Picasso.get().load(R.drawable.ic_action_add_image).into(avatarImageView);
                    }

                    // for cover photo
                    try {
                        // if image is received then set
                        if (cover != null) {
                            Picasso.get().load(cover).into(coverImageView);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadHisPosts();
    }

    private void loadHisPosts() {
        // linear layout for recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        // show newest post first, for this load from last
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);

        // set this layout to recycler view
        postsRecyclerView.setLayoutManager(linearLayoutManager);

        // init posts list
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");

        // query to load posts
        /*
         * Whenever user publishes a post the uid of this user is also saved as info of post
         * So we are retrieving posts having uid equals to uid of current user
         * */
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        // get all data from this databaseReference
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost myPost = ds.getValue(ModelPost.class);

                    // add to list
                    postList.add(myPost);

                    // adapter
                    adapterPosts = new AdapterPosts(postList, ThereProfileActivity.this);
                    // set this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ThereProfileActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchHisPosts(final String searchQuery) {
        // linear layout for recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        // show newest post first, for this load from last
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);

        // set this layout to recycler view
        postsRecyclerView.setLayoutManager(linearLayoutManager);

        // init posts list
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");

        // query to load posts
        /*
         * Whenever user publishes a post the uid of this user is also saved as info of post
         * So we are retrieving posts having uid equals to uid of current user
         * */
        Query query = databaseReference.orderByChild("uid").equalTo(uid);
        // get all data from this databaseReference
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost hisPost = ds.getValue(ModelPost.class);

                    assert hisPost != null;
                    if (hisPost.getPostTitle().toLowerCase().contains(searchQuery.toLowerCase()) || hisPost.getPostDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                        // add to list
                        postList.add(hisPost);

                    }

                    // adapter
                    adapterPosts = new AdapterPosts(postList, ThereProfileActivity.this);
                    // set this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ThereProfileActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserStatus() {
        // get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            // set email of logged in user
            // emailText.setText(user.getEmail());
        } else {
            // user not sign in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    // inflate option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.actionAddPost).setVisible(false); // hide add post from this activity
        MenuItem item = menu.findItem(R.id.actionSearch);
        menu.findItem(R.id.actionSettings).setVisible(false);
        // search view of search user specific post
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // called when user press search button
                if (!TextUtils.isEmpty(query)) {
                    // search
                    searchHisPosts(query);
                } else {
                    loadHisPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // called whenever user type any letter
                if (!TextUtils.isEmpty(newText)) {
                    // search
                    searchHisPosts(newText);
                } else {
                    loadHisPosts();
                }
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    // handle menu item clicks
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
