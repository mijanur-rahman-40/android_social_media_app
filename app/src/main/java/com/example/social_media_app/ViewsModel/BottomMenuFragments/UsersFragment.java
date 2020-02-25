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

import com.example.social_media_app.Adapters.AdapterUsers;
import com.example.social_media_app.Models.ModelUser;
import com.example.social_media_app.R;
import com.example.social_media_app.Views.MainActivity;
import com.example.social_media_app.Views.SettingsActivity;
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


public class UsersFragment extends Fragment {

    private static final String TAG = "";

    private RecyclerView recyclerView;
    AdapterUsers adapterUsers;
    List<ModelUser> userList;

    // firebase auth
    FirebaseAuth firebaseAuth;


    public UsersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        // init
        firebaseAuth = FirebaseAuth.getInstance();

        // init recycler view
        recyclerView = view.findViewById(R.id.usersRecyclerView);

        // set it'AdapterPosts properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));


        // init user list
        userList = new ArrayList<>();

        // get all users
        getAllUsers();

        return view;
    }


    private void getAllUsers() {

        // get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // get path of database named "Users" containing users info
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // get all data from path
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    // get specific value of ModelUser
                    ModelUser modelUser = ds.getValue(ModelUser.class);


                    // get all users except currently signed in user
                    assert modelUser != null;
                    assert user != null;
                    if (!modelUser.getUid().equals(user.getUid())) {
                        userList.add(modelUser);
                    }

                    // adapter
                    adapterUsers = new AdapterUsers(getActivity(), userList);

                    // set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchUsers(final String query) {
        // get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // get path of database named "Users" containing users info
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // get all data from path
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                    /* Conditions to full fill search :
                     * 1)User not current user
                     * 2) The user name or email contains text entered in SearchView (case insensitive)
                     * */

                    // get all search users except currently signed in user
                    assert modelUser != null;
                    assert user != null;
                    if (!modelUser.getUid().equals(user.getUid())) {
                        if (modelUser.getName().toLowerCase().contains(query.toLowerCase()) || modelUser.getEmail().toLowerCase().contains(query.toLowerCase())) {
                            userList.add(modelUser);
                        }
                    }

                    // adapter
                    adapterUsers = new AdapterUsers(getActivity(), userList);

                    // refresh adapter
                    adapterUsers.notifyDataSetChanged();

                    // set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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

        // hide add post icon from this fragment
        menu.findItem(R.id.actionAddPost).setVisible(false);

        // SearchView
        MenuItem item = menu.findItem(R.id.actionSearch);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        // search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // called when user press search button from keyboard
                // if search query is not empty then search
                if (!TextUtils.isEmpty(query.trim())) {
                    // search text contains text,search it
                    searchUsers(query);
                } else {
                    // search text empty,get all users
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // called whenever user press any single letter
                // if search query is not empty then search
                if (!TextUtils.isEmpty(newText.trim())) {
                    // search text contains text,search it
                    searchUsers(newText);
                } else {
                    // search text empty,get all users
                    getAllUsers();
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
        } else if (id == R.id.actionSettings) {
            // go to settings activity
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

}
