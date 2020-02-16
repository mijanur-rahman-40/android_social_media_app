package com.example.social_media_app.ViewsModel.BottomMenuFragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.social_media_app.Adapters.AdapterPosts;
import com.example.social_media_app.Models.ModelPost;
import com.example.social_media_app.R;
import com.example.social_media_app.Views.AddPostActivity;
import com.example.social_media_app.Views.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class HomeFragment extends Fragment {

    private FirebaseAuth firebaseAuth;

    private RecyclerView postRecyclerView;
    private List<ModelPost> postList;
    private AdapterPosts adapterPosts;


    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // init
        firebaseAuth = FirebaseAuth.getInstance();

        // recyclerView and its properties
        postRecyclerView = view.findViewById(R.id.postsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());

        // show newest post first, for this load from last
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);

        // set layout to recycler view
        postRecyclerView.setLayoutManager(linearLayoutManager);

        // init post list
        postList = new ArrayList<>();

        loadPosts();

        return view;
    }

    private void loadPosts() {
        // path of all posts
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");

        // get all data from this reference
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);
                    postList.add(modelPost);

                    // adapter
                    adapterPosts = new AdapterPosts(postList, getActivity());
                    // set adapter to recycler view
                    postRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // in case of error
                Toast.makeText(getActivity(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchPosts(final String searchQuery){
// path of all posts
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");

        // get all data from this reference
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    assert modelPost != null;
                    if (modelPost.getPostTitle().toLowerCase().contains(searchQuery.toLowerCase()) || modelPost.getPostDescription().toLowerCase().contains(searchQuery.toLowerCase())){
                        postList.add(modelPost);
                    }


                    // adapter
                    adapterPosts = new AdapterPosts(postList, getActivity());
                    // set adapter to recycler view
                    postRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // in case of error
                Toast.makeText(getActivity(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
            startActivity(new Intent(getActivity(), MainActivity.class));
            Objects.requireNonNull(getActivity()).finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); // to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    // inflate option menu
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater menuInflater) {
        // inflate menu_main
        menuInflater.inflate(R.menu.menu_main, menu);

        // search view to search posts by posts title/description
        MenuItem item = menu.findItem(R.id.actionSearch);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        // search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // called when user press search button
                if (!TextUtils.isEmpty(query)){
                    searchPosts(query);
                }else {
                    loadPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // called as and when user press any letter
                if (!TextUtils.isEmpty(newText)){
                    searchPosts(newText);
                }else {
                    loadPosts();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, menuInflater);
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
        if (id == R.id.actionAddPost) {
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

}
