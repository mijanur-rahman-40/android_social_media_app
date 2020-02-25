package com.example.social_media_app.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.social_media_app.Models.ModelChat;
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

import java.util.Calendar;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {

    private static final int MESSAGE_TYPE_LEFT = 0;
    private static final int MESSAGE_TYPE_RIGHT = 1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;

    // firebase
    FirebaseUser firebaseUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // inflate layouts: row_chat_left.paths for receiver, row_chat_right.paths for sender
        if (viewType == MESSAGE_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, viewGroup, false);
            return new MyHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, viewGroup, false);
            return new MyHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder myHolder, final int position) {
        // get data
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimestamp();
        String type = chatList.get(position).getType();

        // convert time stamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        if (!TextUtils.isEmpty(timeStamp)) {
            calendar.setTimeInMillis(Long.parseLong(timeStamp));
        }

        //calendar.setTimeInMillis(Long.valueOf(timeStamp));
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        // String timeStamp = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());

        if (type.equals("text")){
            // text message
            myHolder.messageText.setVisibility(View.VISIBLE);
            myHolder.messageImageView.setVisibility(View.GONE);

            myHolder.messageText.setText(message);
        }else {
            // image message
            myHolder.messageText.setVisibility(View.GONE);
            myHolder.messageImageView.setVisibility(View.VISIBLE);

            Picasso.get().load(message).placeholder(R.drawable.ic_image_message_black).into(myHolder.messageImageView);
        }

        // set data

        myHolder.timeText.setText(dateTime);

        try {
            Picasso.get().load(imageUrl).into(myHolder.profileImageView);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // click to show delete dialog
        myHolder.messageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // show delete message conform dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete this message?");

                // delete button
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessage(position);
                    }
                });

                // cancel delete button
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // dialog dismiss
                        dialog.dismiss();
                    }
                });

                // create and show dialog
                builder.create().show();
            }
        });

        // set seen/delivered status of message
        if (position == chatList.size() - 1) {
            if (chatList.get(position).isSeen()) {
                String seen = "Seen";
                myHolder.isSeenText.setText(seen);

            } else {
                String delivered = "Delivered";
                myHolder.isSeenText.setText(delivered);
            }
        } else {
            myHolder.isSeenText.setVisibility(View.GONE);
        }
    }

    private void deleteMessage(int position) {

        final String myUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        /*
         * Get timestamp of clicked message
         * Compare the timestamp of the clicked message with all messages in chats
         * Where both values matches delete that message
         */

        String messageTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = databaseReference.orderByChild("timestamp").equalTo(messageTimeStamp);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    /*
                     * if you want to allow sender to delete only his message then
                     * compare sender value with current user'AdapterPosts id
                     * if they match means its the message of sender that is trying to delete
                     */
                    if (Objects.equals(ds.child("sender").getValue(), myUID)) {

                        /* We can do one of two things here
                         * 1) Remove the messages from data
                         * 2) Set the value of message "This message was deleted.."
                         */

                        // 1) remove the message from chats
                        // ds.getRef().removeValue();

                        // 2) Set the value of message "This message was deleted..."
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "This message was deleted...");
                        ds.getRef().updateChildren(hashMap);

                        Toast.makeText(context, "Message deleted successfully..", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "You can delete only your message!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // get currently signed in user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(firebaseUser.getUid())) {
            return MESSAGE_TYPE_RIGHT;
        } else {
            return MESSAGE_TYPE_LEFT;
        }
    }

    // view holder class
    class MyHolder extends RecyclerView.ViewHolder {

        // views
        ImageView profileImageView,messageImageView;
        TextView messageText, timeText, isSeenText;
        LinearLayout messageLayout; // for clicking listener to show delete


        public MyHolder(@NonNull View itemView) {
            super(itemView);

            // init views
            profileImageView = itemView.findViewById(R.id.profileImage);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeTextView);
            isSeenText = itemView.findViewById(R.id.isSeenTextView);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }
}
