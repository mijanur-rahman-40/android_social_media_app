package com.example.social_media_app.Adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.social_media_app.Configuration.Date;
import com.example.social_media_app.Models.ModelPost;
import com.example.social_media_app.R;
import com.example.social_media_app.Views.AddPostActivity;
import com.example.social_media_app.Views.PostDetailActivity;
import com.example.social_media_app.Views.ThereProfileActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder> {

    private List<ModelPost> postList;
    private Context context;
    private String myUid;

    private DatabaseReference likesReference; // for likes database node
    private DatabaseReference postsReference;  // reference for posts

    private boolean myProcessLike = false;

    public AdapterPosts(List<ModelPost> postList, Context context) {
        this.postList = postList;
        this.context = context;
        this.myUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        this.likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");
        this.postsReference = FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // inflate layout row_posts.paths
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, viewGroup, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder myHolder, final int position) {

        // get data
        final String uid, userName, userEmail, userProfile, postId, postTitle, postDescription, postImage, postTime, postLikes, postComments;

        uid = postList.get(position).getUid();
        userName = postList.get(position).getUserName();
        userEmail = postList.get(position).getUserEmail();
        userProfile = postList.get(position).getUserProfile();
        postId = postList.get(position).getPostId();
        postTitle = postList.get(position).getPostTitle();
        postDescription = postList.get(position).getPostDescription();
        postImage = postList.get(position).getPostImage();
        postTime = postList.get(position).getPostTime();
        postLikes = postList.get(position).getPostLikes();
        postComments = postList.get(position).getPostComments();

        // convert timestamp to dd/mm/yyyy hh:mm am/pm
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        if (!TextUtils.isEmpty(postTime)) {
            calendar.setTimeInMillis(Long.parseLong(postTime));
        }
        String time = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        String[] splitTime = time.split("/", 3);
        Date date = new Date(Integer.parseInt(splitTime[1]));
        String[] split = splitTime[2].split(" ", 2);
        String originalTime = splitTime[0] + " " + date.getMonthName() + " | " + split[1];

        // set data
        if (userName != null) {
            myHolder.userName.setText(userName);
        }

        myHolder.postTime.setText(originalTime);
        myHolder.postTitle.setText(postTitle);
        myHolder.postDescription.setText(postDescription);
        myHolder.postLikes.setText(postLikes + " Likes");
        myHolder.postComments.setText(postComments + " Comments");

        // set likes for each post
        setLikes(myHolder, postId);

        // set user image
        try {
            Picasso.get().load(userProfile).placeholder(R.drawable.ic_default_image).into(myHolder.profileImage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // set post image
        // if there is no image i.e postImage.equals("noImage") then hide image View
        if (postImage.equals("noImage")) {
            // hide image view
            myHolder.postImage.setVisibility(View.GONE);
        } else {
            // show image view
            myHolder.postImage.setVisibility(View.VISIBLE);

            try {
                Picasso.get().load(postImage).into(myHolder.postImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        // handle button clicks
        myHolder.moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions(myHolder.moreButton, uid, myUid, postId, postImage);
            }
        });

        myHolder.likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * get total number of likes for the post, whose like button clicked
                 * if currently signed in user has not liked it before
                 * increase value by 1, otherwise decrease by 1
                 * */

                final int postLikes = Integer.parseInt(postList.get(position).getPostLikes());
                myProcessLike = true;

                // get id of the post clicked
                final String postID = postList.get(position).getPostId();

                likesReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (myProcessLike) {
                            if (dataSnapshot.child(postID).hasChild(myUid)) {
                                // already liked, so remove like
                                postsReference.child(postID).child("postLikes").setValue("" + (postLikes - 1));
                                likesReference.child(postID).child(myUid).removeValue();
                                myProcessLike = false;
                            } else {
                                // not liked , like it
                                postsReference.child(postID).child("postLikes").setValue("" + (postLikes + 1));
                                likesReference.child(postID).child(myUid).setValue("Liked"); // set any value
                                myProcessLike = false;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(context, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });

        myHolder.commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // intent post detail activity
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", postId); // will get detail of post using the id,its id of post clicked

                context.startActivity(intent);
            }
        });

        myHolder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // share post of text and image
                BitmapDrawable bitmapDrawable = (BitmapDrawable) myHolder.postImage.getDrawable();
                if (bitmapDrawable == null) {
                    // post without image
                    shareTextOnly(postTitle, postDescription);
                } else {
                    // post with image
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(postTitle, postDescription, bitmap);
                }
            }
        });

        myHolder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * click to go to thereProfileActivity with uid, this uid is of clicked user.
                 * which will be used to show user specific data/posts
                 * */

                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid", uid);
                context.startActivity(intent);
            }
        });
    }

    private void shareTextOnly(String postTitle, String postDescription) {

        // concatenate title and description to share
        String shareBody = postTitle + "\n" + postDescription;

        // share intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here"); // in case you share via an email app
        intent.putExtra(Intent.EXTRA_TEXT, shareBody); // text to share
        context.startActivity(Intent.createChooser(intent, "Share Via")); // message to show in share dialog
    }

    private void shareImageAndText(String postTitle, String postDescription, Bitmap bitmap) {
        // concatenate title and description to share
        String shareBody = postTitle + "\n" + postDescription;

        // first we will save this image in cache, get the saved image url
        Uri uri = saveImageToShare(bitmap);

        // share intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        intent.setType("image/png");
        context.startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try {
            imageFolder.mkdirs(); // create if not exists
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            uri = FileProvider.getUriForFile(context, "com.example.social_media_app.fileprovider", file);

        } catch (Exception e) {
            Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }



    private void setLikes(final MyHolder myHolder, final String postKey) {
        likesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postKey).hasChild(myUid)) {
                    // user has liked this post
                    /*
                     * To indicate that the post is liked by this(SignedIn) user
                     * Change drawable left icon of like button
                     * Change text of like button from "Like" to "Liked"
                     * */
                    myHolder.likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    myHolder.likeButton.setText("Liked");
                } else {
                    // user has not liked this post

                    /*
                     * To indicate that the post is not liked by this(SignedIn) user
                     * Change drawable left icon of like button
                     * Change text of like button from "Liked" to "Like"
                     * */
                    myHolder.likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_up_black, 0, 0, 0);
                    myHolder.likeButton.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMoreOptions(ImageButton moreButton, String uid, String myUid, final String postId, final String postImage) {

        // creating popup menu currently having option delete
        PopupMenu popupMenu = new PopupMenu(context, moreButton, Gravity.END);

        // show delete option only post(s)of currently signed user
        if (uid.equals(myUid)) {
            // add item in menu
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
        }

        popupMenu.getMenu().add(Menu.NONE, 2, 0, "View Detail");

        // item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 0) {
                    // delete ic clicked
                    beginDelete(postId, postImage);
                } else if (id == 1) {
                    // edit is clicked
                    // start AddPostActivity with key "editPost" and id of the post click
                    Intent intent = new Intent(context, AddPostActivity.class);
                    intent.putExtra("key", "editPost");
                    intent.putExtra("editPostId", postId);
                    context.startActivity(intent);

                } else if (id == 2) {
                    // intent post detail activity
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId", postId); // will get detail of post using the id,its id of post clicked
                    context.startActivity(intent);
                }
                return false;
            }
        });
        // show menu
        popupMenu.show();
    }

    private void beginDelete(String postId, String postImage) {

        // post can be with or without image
        if (postImage.equals("noImage")) {
            // post is without image
            deleteWithoutImage(postId);
        } else {
            // post is with image
            deleteWithImage(postId, postImage);
        }
    }

    private void deleteWithImage(final String postId, String postImage) {
        // progress bar
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Deleting...");

        /*
         * Steps
         * 1) Delete image with url
         * 2) Delete from database using postId
         * */

        StorageReference pictureReference = FirebaseStorage.getInstance().getReferenceFromUrl(postImage);
        pictureReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        // image deleted , now delete database
                        Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("postId").equalTo(postId);

                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    ds.getRef().removeValue(); // remove value from firebase where pid matches
                                }
                                // deleted
                                Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // failed, can not go further
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void deleteWithoutImage(String postId) {

        // progress bar
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Deleting...");

        // image deleted , now delete database
        Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("postId").equalTo(postId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ds.getRef().removeValue(); // remove value from firebase where pid matches
                }
                // deleted
                Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // view holder class
    static class MyHolder extends RecyclerView.ViewHolder {

        // views from row_post.paths
        ImageView postImage, profileImage;
        TextView userName;
        TextView postTime, postTitle, postDescription, postLikes, postComments;
        ImageButton moreButton;
        Button likeButton, commentButton, shareButton;
        LinearLayout profileLayout;


        MyHolder(@NonNull View itemView) {
            super(itemView);

            // init views
            postImage = itemView.findViewById(R.id.postImageShowView);
            profileImage = itemView.findViewById(R.id.userPictureImageView);

            userName = itemView.findViewById(R.id.userNameTextView);
            postTime = itemView.findViewById(R.id.postTimeTextView);
            postTitle = itemView.findViewById(R.id.postTitleTextView);
            postDescription = itemView.findViewById(R.id.postDescriptionTextView);
            postLikes = itemView.findViewById(R.id.postLikeTextView);
            postComments = itemView.findViewById(R.id.postCommentsTextView);

            moreButton = itemView.findViewById(R.id.moreButton);

            likeButton = itemView.findViewById(R.id.likeButton);
            commentButton = itemView.findViewById(R.id.commentButton);
            shareButton = itemView.findViewById(R.id.shareButton);

            profileLayout = itemView.findViewById(R.id.profileLayout);
        }
    }
}
