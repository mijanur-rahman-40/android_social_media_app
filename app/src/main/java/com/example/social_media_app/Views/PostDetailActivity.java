package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.social_media_app.Adapters.AdapterComments;
import com.example.social_media_app.Models.ModelComment;
import com.example.social_media_app.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.sql.StatementEvent;

public class PostDetailActivity extends AppCompatActivity {

    // to get detail of user and post
    String myUid, myEmail, myName, myProfile;
    String postId, postLikes, hisProfile, hisName, hisUid, postImage;

    boolean myProcessComment = false;
    private boolean myProcessLike = false;

    // progress bar
    ProgressDialog progressDialog;

    // views
    ImageView userPictureImage, postPictureImage;
    TextView userNameView, postTimeView, postTitleView, postDescriptionView, postLikesView, postCommentsTextView;

    Button likeButton, shareButton;
    ImageButton moreButton;

    LinearLayout profileLayout;
    RecyclerView recyclerView;
    List<ModelComment> commentList;
    AdapterComments adapterComments;

    // add comment views
    EditText commentEditText;
    ImageButton commentSendButton;
    ImageView commentAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // actionbar and its properties
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // get id of post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");


        // init views
        userPictureImage = findViewById(R.id.userPictureImageView);
        postPictureImage = findViewById(R.id.postImageShowView);

        userNameView = findViewById(R.id.userNameTextView);
        postTimeView = findViewById(R.id.postTimeTextView);
        postTitleView = findViewById(R.id.postTitleTextView);
        postDescriptionView = findViewById(R.id.postDescriptionTextView);
        postLikesView = findViewById(R.id.postLikeTextView);
        postCommentsTextView = findViewById(R.id.postCommentsTextView);

        likeButton = findViewById(R.id.likeButton);
        shareButton = findViewById(R.id.shareButton);
        moreButton = findViewById(R.id.moreButton);

        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.commentRecyclerView);

        commentEditText = findViewById(R.id.commentEditText);
        commentSendButton = findViewById(R.id.commentSendButton);
        commentAvatar = findViewById(R.id.commentAvatarImageView);

        loadPostInfo();
        checkUserStatus();
        loadUserInfo();
        setLikes();

        // set subtitle of actionbar
        actionBar.setSubtitle("Signed as : " + myEmail);

        loadComments();

        // send comment button click
        commentSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });

        // like button click handle
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });

        // more button click handle
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });


        // share button click handle
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String postTitle = postTitleView.getText().toString().trim();
                String postDescription = postDescriptionView.getText().toString().trim();

                // share post of text and image
                BitmapDrawable bitmapDrawable = (BitmapDrawable) postPictureImage.getDrawable();
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
    }

    private void shareTextOnly(String postTitle, String postDescription) {

        // concatenate title and description to share
        String shareBody = postTitle + "\n" + postDescription;

        // share intent
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here"); // in case you share via an email app
        intent.putExtra(Intent.EXTRA_TEXT, shareBody); // text to share
        startActivity(Intent.createChooser(intent, "Share Via")); // message to show in share dialog
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
        startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imageFolder.mkdirs(); // create if not exists
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();

            uri = FileProvider.getUriForFile(PostDetailActivity.this, "com.example.social_media_app.fileprovider", file);

        } catch (Exception e) {
            Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return uri;
    }

    private void loadComments() {
        // layout(linear) for recyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        // set layout to recycler view
        recyclerView.setLayoutManager(layoutManager);

        // init comment list
        commentList = new ArrayList<>();

        // path to the post to get its comments
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelComment modelComment = ds.getValue(ModelComment.class);

                    commentList.add(modelComment);

                    // pass uid and postId as parameter of constructor of Comment Adapter

                    // set up adapter
                    adapterComments = new AdapterComments(getApplicationContext(), commentList, myUid, postId);
                    // set adapter
                    recyclerView.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMoreOptions() {
        // creating popup menu currently having option delete
        PopupMenu popupMenu = new PopupMenu(this, moreButton, Gravity.END);

        // show delete option only post(s)of currently signed user
        if (hisUid.equals(myUid)) {
            // add item in menu
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
        }


        // item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 0) {
                    // delete ic clicked
                    beginDelete();
                } else if (id == 1) {
                    // edit is clicked
                    // start AddPostActivity with key "editPost" and id of the post click
                    Intent intent = new Intent(PostDetailActivity.this, AddPostActivity.class);
                    intent.putExtra("key", "editPost");
                    intent.putExtra("editPostId", postId);
                    startActivity(intent);

                }
                return false;
            }
        });
        // show menu
        popupMenu.show();
    }

    private void beginDelete() {

        // post can be with or without image
        if (postImage.equals("noImage")) {
            // post is without image
            deleteWithoutImage();
        } else {
            // post is with image
            deleteWithImage();
        }
    }

    private void deleteWithoutImage() {
        // progress bar
        final ProgressDialog progressDialog = new ProgressDialog(this);
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
                Toast.makeText(PostDetailActivity.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteWithImage() {
        // progress bar
        final ProgressDialog progressDialog = new ProgressDialog(this);
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
                                Toast.makeText(PostDetailActivity.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void setLikes() {
        // when the details of post is loading, also check if current user has liked it or not
        final DatabaseReference likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");

        likesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)) {

                    // user has liked this post

                    /*
                     * To indicate that the post is liked by this(SignedIn) user
                     * Change drawable left icon of like button
                     * Change text of like button from "Like" to "Liked"
                     * */
                    likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    likeButton.setText("Liked");
                } else {
                    // user has not liked this post

                    /*
                     * To indicate that the post is not liked by this(SignedIn) user
                     * Change drawable left icon of like button
                     * Change text of like button from "Liked" to "Like"
                     * */
                    likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_up_black, 0, 0, 0);
                    likeButton.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void likePost() {
        /*
         * get total number of likes for the post, whose like button clicked
         * if currently signed in user has not liked it before
         * increase value by 1, otherwise decrease by 1
         * */

        myProcessLike = true;
        final DatabaseReference likesReference = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsReference = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (myProcessLike) {
                    if (dataSnapshot.child(postId).hasChild(myUid)) {
                        // already liked, so remove like
                        postsReference.child(postId).child("postLikes").setValue("" + (Integer.parseInt(postLikes) - 1));
                        likesReference.child(postId).child(myUid).removeValue();
                        myProcessLike = false;
                    } else {
                        // not liked , like it
                        postsReference.child(postId).child("postLikes").setValue("" + (Integer.parseInt(postLikes) + 1));
                        likesReference.child(postId).child(myUid).setValue("Liked"); // set any value
                        myProcessLike = false;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postComment() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding comment...");

        // get data from comment edit text
        String comment = commentEditText.getText().toString().trim();

        // validate
        if (TextUtils.isEmpty(comment)) {
            // no value is entered
            Toast.makeText(this, "Comment is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String timeStamp = String.valueOf(System.currentTimeMillis());
        // each post will have a child "Comments" that will contains of that post
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String, Object> hashMap = new HashMap<>();
        // put info in hash map
        hashMap.put("commentId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("userEmail", myEmail);
        hashMap.put("userProfile", myProfile);
        hashMap.put("userName", myName);

        // put this data into db
        databaseReference.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // added
                        progressDialog.dismiss();
                        Toast.makeText(PostDetailActivity.this, "Comment Added...", Toast.LENGTH_SHORT).show();
                        commentEditText.setText("");
                        updateCommentCount();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        // failed, not added
                        Toast.makeText(PostDetailActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


    }


    private void updateCommentCount() {
        // whenever user adds comment increase the comment count as we did for like count
        myProcessComment = true;
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts").child(postId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (myProcessComment) {
                    String comments = "" + dataSnapshot.child("postComments").getValue();
                    int newCommentValue = Integer.parseInt(comments) + 1;
                    databaseReference.child("postComments").setValue("" + newCommentValue);
                    myProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadUserInfo() {
        // get current user info
        Query myReference = FirebaseDatabase.getInstance().getReference("Users");
        myReference.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    myName = "" + ds.child("name").getValue();
                    myProfile = "" + ds.child("image").getValue();

                    // set data into comment part
                    try {
                        // if image is received then user
                        Picasso.get().load(myProfile).placeholder(R.drawable.ic_default_image).into(commentAvatar);
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default_image).into(commentAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPostInfo() {
        // get post using the id of the post
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = databaseReference.orderByChild("postId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // keep checking the post until get the required post
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    // get data
                    // put post information

                    String postTitle = "" + ds.child("postTitle").getValue();
                    String postDescription = "" + ds.child("postDescription").getValue();
                    postLikes = "" + ds.child("postLikes").getValue();
                    String postTimeStamp = "" + ds.child("postTime").getValue();
                    postImage = "" + ds.child("postImage").getValue();
                    hisProfile = "" + ds.child("userProfile").getValue();
                    hisUid = "" + ds.child("uid").getValue();
                    hisName = "" + ds.child("userName").getValue();
                    String commentCount = "" + ds.child("postComments").getValue();

                    // convert timeStamp to proper format
                    // convert timestamp to dd/mm/yyyy hh:mm am/pm
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    if (!TextUtils.isEmpty(postTimeStamp)) {
                        calendar.setTimeInMillis(Long.parseLong(postTimeStamp));
                    }
                    String time = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                    // set data
                    postTitleView.setText(postTitle);
                    postDescriptionView.setText(postDescription);
                    postLikesView.setText(postLikes + " Likes");
                    postTimeView.setText(time);
                    postCommentsTextView.setText(commentCount + " Comments");

                    userNameView.setText(hisName);

                    if (postImage.equals("noImage")) {
                        // hide image view
                        postPictureImage.setVisibility(View.GONE);
                    } else {
                        // show image view
                        postPictureImage.setVisibility(View.VISIBLE);

                        try {
                            Picasso.get().load(postImage).into(postPictureImage);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        Picasso.get().load(hisProfile).placeholder(R.drawable.ic_default_image).into(userPictureImage);
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.ic_default_image).into(userPictureImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserStatus() {
        // get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            // set email of logged in user
            myEmail = user.getEmail();
            myUid = user.getUid();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.actionAddPost).setVisible(false);
        menu.findItem(R.id.actionSearch).setVisible(false);
        menu.findItem(R.id.actionSettings).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // get item id
        int id = item.getItemId();
        if (id == R.id.actionLogout) {
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

}
