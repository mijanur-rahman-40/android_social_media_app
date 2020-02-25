package com.example.social_media_app.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.social_media_app.Models.ModelComment;
import com.example.social_media_app.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.MyHolder> {
    private Context context;
    private List<ModelComment> commentsList;
     String myUid, postId;

    public AdapterComments(Context context, List<ModelComment> commentsList, String myUid, String postId) {
        this.context = context;
        this.commentsList = commentsList;
        this.myUid = myUid;
        this.postId = postId;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // bind the row_comments.paths layout
        View view = LayoutInflater.from(context).inflate(R.layout.row_comments, viewGroup, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder myHolder, int position) {

        // get the data
        final String uid = commentsList.get(position).getUid();
        final String commentID = commentsList.get(position).getCommentId();
        String comment = commentsList.get(position).getComment();
        String timeStamp = commentsList.get(position).getTimestamp();
        String userName = commentsList.get(position).getUserName();
        String userEmail = commentsList.get(position).getUserEmail();
        String userProfile = commentsList.get(position).getUserProfile();

        // set the data
        // convert timestamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        if (!TextUtils.isEmpty(timeStamp)) {
            calendar.setTimeInMillis(Long.parseLong(timeStamp));
        }
        String time = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        // set data
        myHolder.nameText.setText(userName);
        myHolder.commentText.setText(comment);
        myHolder.timeText.setText(time);

        // set the user profile
        try {
            Picasso.get().load(userProfile).placeholder(R.drawable.ic_default_image).into(myHolder.avatarImageView);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // comment click listener
        myHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check if this comment id by currently signed in user or not
                if (myUid.equals(uid)) {
                    // my comment
                    // show delete dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());
                    builder.setTitle("Delete");
                    builder.setMessage("Are you sure to delete this comments?");
                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // delete comment
                            deleteComment(commentID);
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // dismiss dialog
                            dialog.dismiss();
                        }
                    });
                    // show dialog
                    builder.create().show();

                } else {
                    // no my comment
                    Toast.makeText(context, "Can not delete other comments...", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteComment(String commentID) {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        reference.child("Comments").child(commentID).removeValue(); // it will delete the comment

        // now update the comments count
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String comments = "" + dataSnapshot.child("postComments").getValue();
                int newCommentValue = Integer.parseInt(comments) - 1;
                reference.child("postComments").setValue("" + newCommentValue);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    // view holder class
    class MyHolder extends RecyclerView.ViewHolder {

        // declare views from row_comments.paths
        ImageView avatarImageView;
        TextView nameText, commentText, timeText;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            avatarImageView = itemView.findViewById(R.id.commentAvatar);
            nameText = itemView.findViewById(R.id.commentUserNameTextView);
            commentText = itemView.findViewById(R.id.commentTextView);
            timeText = itemView.findViewById(R.id.postCommentTimeTextView);
        }
    }
}
