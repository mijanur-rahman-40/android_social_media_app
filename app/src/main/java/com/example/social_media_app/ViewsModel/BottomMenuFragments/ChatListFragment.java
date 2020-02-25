package com.example.social_media_app.ViewsModel.BottomMenuFragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
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

import com.example.social_media_app.Adapters.AdapterChatList;
import com.example.social_media_app.Models.ModelChat;
import com.example.social_media_app.Models.ModelChatList;
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


public class ChatListFragment extends Fragment {


    RecyclerView chatListRecyclerView;
    List<ModelChatList> chatLists;
    List<ModelUser> userList;
    AdapterChatList adapterChatList;

    FirebaseAuth firebaseAuth;
    DatabaseReference databaseReference;
    FirebaseUser currentUser;

    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        // init
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        chatListRecyclerView = view.findViewById(R.id.chatListRecyclerView);

        chatLists = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("ChatList").child(currentUser.getUid());
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatLists.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChatList chatList = ds.getValue(ModelChatList.class);
                    chatLists.add(chatList);
                }
                loadChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return view;
    }

    private void loadChats() {
        userList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser user = ds.getValue(ModelUser.class);
                    for (ModelChatList chatList : chatLists) {
                        assert user != null;
                        if (user.getUid() != null && user.getUid().equals(chatList.getId())) {
                            userList.add(user);
                            break;
                        }
                    }
                    // adapter
                    adapterChatList = new AdapterChatList(getContext(), userList);
                    // set adapter
                    chatListRecyclerView.setAdapter(adapterChatList);
                    // set last message
                    for (int i = 0; i < userList.size(); i++) {
                        lastMessage(userList.get(i).getUid());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void lastMessage(final String uid) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String theLastMessage = "default";
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat == null) {
                        continue;
                    }
                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();

                    if (sender == null || receiver == null) {
                        continue;
                    }
                    if (chat.getSender().equals(currentUser.getUid()) && chat.getSender().equals(uid) || chat.getReceiver().equals(uid) && chat.getSender().equals(currentUser.getUid())) {
                        // instead of displaying url in messaging show "sent photo"
                        if (chat.getType().equals("image")) {
                            theLastMessage = "Sent a photo";

                        } else {
                            theLastMessage = chat.getMessage();
                        }

                    }
                }
                adapterChatList.setLastMessageMap(uid, theLastMessage);
                adapterChatList.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
        }else if (id == R.id.actionSettings) {
            // go to settings activity
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}



