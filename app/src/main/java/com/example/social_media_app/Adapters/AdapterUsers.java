package com.example.social_media_app.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.social_media_app.Models.ModelUser;
import com.example.social_media_app.R;
import com.example.social_media_app.Views.ChatActivity;
import com.example.social_media_app.Views.ThereProfileActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyUserHolder> {

    private Context context;
    private List<ModelUser> userList;

    // for getting current user id's
    private FirebaseAuth firebaseAuth;
    private String myUid;

    public AdapterUsers(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyUserHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        // inflate layout(row_user.paths)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, viewGroup, false);

        return new MyUserHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyUserHolder myHolder, final int position) {

        // get data
        final String hisUID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        // set data
        myHolder.nameText.setText(userName);
        myHolder.emailText.setText(userEmail);

        try {
            Picasso.get()
                    .load(userImage)
                    .placeholder(R.drawable.ic_default_image)
                    .into(myHolder.myAvatarImage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        myHolder.blockImage.setImageResource(R.drawable.ic_unblocked_green);

        // check if each user if is blocked or not
        checkIsBlocked(hisUID, myHolder, position);

        // handle item click
        myHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile", "Chat"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {

                            // profile clicked
                            /*
                             * click to go to thereProfileActivity with uid, this uid is of clicked user.
                             * which will be used to show user specific data/posts
                             * */

                            Intent intent = new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid", hisUID);
                            context.startActivity(intent);
                        }
                        if (which == 1) {
                            // chat clicked
                            /* Click user from user to start chatting/messaging
                             * Start activity by putting UID of receiver
                             * We will use that UID to identify the user we are gonna chat
                             * */
                            isBlockedOrNot(hisUID);
                        }
                    }
                });
                builder.create().show();
            }
        });

        myHolder.blockImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userList.get(position).isBlocked()) {
                    unBlockedUser(hisUID);
                } else {
                    blockedUser(hisUID);
                }
            }
        });
    }

    private void isBlockedOrNot(final String hisUid) {
        // first check if sender(current user) is blocked by receiver or not
        // Logic: if uid of the sender(current user) exists in "BlockedUsers" of receiver then sender(current user) is blocked, otherwise not
        // if blocked then just display a message e.g You are blocked by that user, can not send message
        // if not blocked then simply start the chat activity

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.child(hisUid).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.exists()) {
                                Toast.makeText(context, "You are blocked by that user, can not send message", Toast.LENGTH_SHORT).show();
                                // blocked, do not proceed further
                                return;
                            }
                        }
                        // not blocked, start activity
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUID", hisUid);
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(context, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIsBlocked(String hisUID, final MyUserHolder myHolder, final int position) {
        // check each user , if blocked or not
        // if uid of the user exists in "BlockedUsers" then that user is blocked, otherwise not

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.exists()) {
                                myHolder.blockImage.setImageResource(R.drawable.ic_blocked_red);
                                userList.get(position).setBlocked(true);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(context, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void blockedUser(String hisUID) {
        // block the user, By adding uid to current user's "BlockedUsers" node
        // put value into hash map to put in db
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUID);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.child(myUid).child("BlockedUsers").child(hisUID).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // blocked successfully
                        Toast.makeText(context, "Blocked successfully...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // failed to block
                        Toast.makeText(context, "Failed : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockedUser(String hisUID) {
        // block the user, By removing uid to current user's "BlockedUsers" node
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        databaseReference.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.exists()) {
                                // remove blocked user data from current user's BlockedUsers list
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // unblocked successfully
                                                Toast.makeText(context, "Unblocked successfully...", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // failed to unblock
                                                Toast.makeText(context, "Failed : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(context, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // view holder classes
    static class MyUserHolder extends RecyclerView.ViewHolder {

        ImageView myAvatarImage, blockImage;
        TextView nameText, emailText;

        MyUserHolder(@NonNull View itemView) {
            super(itemView);
            // init views
            myAvatarImage = itemView.findViewById(R.id.avatarImageView);
            nameText = itemView.findViewById(R.id.nameTextView);
            emailText = itemView.findViewById(R.id.emailTextView);
            blockImage = itemView.findViewById(R.id.blockImageView);
        }
    }
}
