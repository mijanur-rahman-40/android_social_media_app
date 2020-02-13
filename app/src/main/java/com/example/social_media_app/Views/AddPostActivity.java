package com.example.social_media_app.Views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.social_media_app.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Objects;

public class AddPostActivity extends AppCompatActivity {

    ActionBar actionBar;
    FirebaseAuth firebaseAuth;
    DatabaseReference userDatabaseReference;

    // user info
    String name, email, uid, image;

    // permissions constants
    private static final int CAMERA_REQUEST_CODE = 80;
    private static final int STORAGE_REQUEST_CODE = 90;

    // image pick constants
    private static final int IMAGE_PICK_CAMERA_CODE = 50;
    private static final int IMAGE_PICK_GALLERY_CODE = 60;

    // permission array
    String[] cameraPermissions;
    String[] storagePermissions;

    // image picked will be samed in this uri
    Uri imageUri = null;

    // views
    EditText titleEditText, descriptionEditText;
    ImageView postImageView;
    Button uploadButton;

    // progress bar
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Add New Post");

        // enable back button in action bar
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // init permissions arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        actionBar.setSubtitle(email);

        // get some information of current user to include in the post
        userDatabaseReference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDatabaseReference.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    name = "" + ds.child("name").getValue();
                    email = "" + ds.child("email").getValue();
                    image = "" + ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // init views
        titleEditText = findViewById(R.id.postTitleEditText);

        descriptionEditText = findViewById(R.id.postDescriptionEditText);

        postImageView = findViewById(R.id.postImageView);

        uploadButton = findViewById(R.id.postUploadButton);

        // get image from camera/gallery on click
        postImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show image pick dialog
                showImagePickDialog();
            }
        });

        // upload button click listener
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get data (title,description) from editText
                String title = titleEditText.getText().toString().trim();
                String description = descriptionEditText.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(AddPostActivity.this, "Enter title..", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(description)) {
                    Toast.makeText(AddPostActivity.this, "Enter description..", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (imageUri == null) {
                    // post without image
                    uploadData(title, description, "noImage");
                } else {
                    // post with image
                    uploadData(title, description, String.valueOf(imageUri));
                }
            }
        });
    }

    private void uploadData(final String title, final String description, String uri) {
        progressDialog.setMessage("Publishing post..");
        progressDialog.show();

        // for post-image name,post-id,post-publish-time
        final String timeStamp = String.valueOf(System.currentTimeMillis());

        String filePathAndName = "Posts/" + "post_" + timeStamp;
        if (!uri.equals("noImage")) {

            // post with image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            storageReference.putFile(Uri.parse(uri))
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            // image is uploaded to firebase storage, now get it's url
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful()) ;

                            String downloadUri = uriTask.getResult().toString();

                            if (uriTask.isSuccessful()) {
                                // uri is received upload post to firebase database
                                HashMap<Object, String> hashMap = new HashMap<>();

                                // put post information
                                hashMap.put("uid", uid);
                                hashMap.put("userName", name);
                                hashMap.put("userEmail", email);
                                hashMap.put("userProfile", image);
                                hashMap.put("postId", timeStamp);
                                hashMap.put("postTitle", title);
                                hashMap.put("postDescription", description);
                                hashMap.put("postImage", downloadUri);
                                hashMap.put("postTime", timeStamp);

                                // path to store post data
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                                // put data in this reference
                                databaseReference.child(timeStamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // added to database
                                                progressDialog.dismiss();
                                                Toast.makeText(AddPostActivity.this, "Post published", Toast.LENGTH_SHORT).show();

                                                // reset views
                                                titleEditText.setText("");
                                                descriptionEditText.setText("");
                                                postImageView.setImageURI(null);
                                                imageUri = null;
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // failed adding post into database
                                                progressDialog.dismiss();
                                                Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // failed uploading image
                    progressDialog.dismiss();
                    Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // post without image

            HashMap<Object, String> hashMap = new HashMap<>();

            // put post information
            hashMap.put("uid", uid);
            hashMap.put("userName", name);
            hashMap.put("userEmail", email);
            hashMap.put("userProfile", image);
            hashMap.put("postId", timeStamp);
            hashMap.put("postTitle", title);
            hashMap.put("postDescription", description);
            hashMap.put("postImage", "noImage");
            hashMap.put("postTime", timeStamp);

            // path to store post data
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
            // put data in this reference
            databaseReference.child(timeStamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // added to database
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, "Post published", Toast.LENGTH_SHORT).show();
                            // reset views
                            titleEditText.setText("");
                            descriptionEditText.setText("");
                            postImageView.setImageURI(null);
                            imageUri = null;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // failed adding post into database
                            progressDialog.dismiss();
                            Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showImagePickDialog() {
        // options(camera,gallery) to show in dialog
        String[] options = {"Camera", "Gallery"};

        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");

        // set options to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // item click handle
                if (which == 0) {
                    // camera clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }
                }
                if (which == 1) {
                    // gallery clicked
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });

        // create and show dialog
        builder.create().show();

    }

    private void pickFromGallery() {
        // pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        // intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pick");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        // put image uri
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }


    private boolean checkStoragePermission() {
        // check if storage permission is enabled or not
        // return true if enabled
        // return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        // request runtime storage permission
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        // check if Camera permission is enabled or not
        // return true if enabled
        // return false if not enabled
        boolean resultCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean resultCameraStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return resultCameraPermission && resultCameraStorage;
    }

    private void requestCameraPermission() {
        // request runtime storage permission
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    private void checkUserStatus() {
        // get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            // set email of logged in user
            email = user.getEmail();
            uid = user.getUid();
        } else {
            // user not sign in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // go to previous page
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.actionAddPost).setVisible(false);
        menu.findItem(R.id.actionSearch).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

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

    // handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // this method is called when user press Allow or Deny from permission request dialog
        // here we will handle permission cases (allowed and denied)

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        // both permission are granted
                        pickFromCamera();
                    } else {
                        // camera or gallery or both permission were denied
                        Toast.makeText(this, "Camera and Storage both permissions are necessary..", Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        // storage permission granted
                        pickFromGallery();
                    } else {
                        // camera or gallery or both permission were denied
                        Toast.makeText(this, "Storage permissions  necessary..", Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // this method will be called after picking image from camera or gallery
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // image is picked from gallery, get uri of image
                assert data != null;
                imageUri = data.getData();

                // set to image view
                postImageView.setImageURI(imageUri);

            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // image is picked from camera,get uri of image
                postImageView.setImageURI(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
