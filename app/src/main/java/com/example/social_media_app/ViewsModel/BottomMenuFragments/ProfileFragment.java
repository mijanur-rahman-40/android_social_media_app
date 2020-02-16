package com.example.social_media_app.ViewsModel.BottomMenuFragments;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.social_media_app.Adapters.AdapterPosts;
import com.example.social_media_app.Models.ModelPost;
import com.example.social_media_app.R;
import com.example.social_media_app.Views.AddPostActivity;
import com.example.social_media_app.Views.MainActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    // firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    // storage
    private StorageReference storageReference;
    // path where images of user profile and cover will be stored
    String storagePath = "Users_Profile_Cover_Images/";

    // views from xml
    private ImageView avatarImageView, coverImageView;
    private TextView nameText, emailText, phoneText;

    private FloatingActionButton floatingActionButton;

    private RecyclerView postsRecyclerView;

    // progress dialog
    ProgressDialog progressDialog;

    /*
     * fOR PICKING IMAGE REQUIRED PERMISSION
     * CAMERA OPEN AND STORAGE
     * GALLERY STORAGE
     * */

    // permissions constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    // arrays of permission to be requested
    private String[] cameraPermission;
    private String[] storagePermission;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    // uri of picked image
    private Uri imageUri;

    // for checking profile or cover photo
    private String profileOrCoverPhoto;

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference = getInstance().getReference(); // firebase storage reference


        // init arrays of permissions
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        // init views
        avatarImageView = view.findViewById(R.id.avatarImage);
        coverImageView = view.findViewById(R.id.coverImage);

        nameText = view.findViewById(R.id.nameText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);

        floatingActionButton = view.findViewById(R.id.floatingButton);

        postsRecyclerView = view.findViewById(R.id.recyclerViewPosts);

        // init progress dialog
        progressDialog = new ProgressDialog(getActivity());

        /* We have to get into of currently signed in user.We can get it users email or uid.I am going to retrieve user detail using email*/
        /*
         * By using orderByChild query we will show the detail from a  node whose key named email has value equal to currently signed in email.
         * It will search all nodes,whose the key matches it will get the detail
         * */
        Query query = databaseReference.orderByChild("email").equalTo(firebaseUser.getEmail());
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
                    String cover = "" + data_snapshot.child("cover").getValue();

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
                        Picasso.get().load(cover).into(coverImageView);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // floating action button click
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadMyPosts();
        return view;
    }

    private void loadMyPosts() {
        // linear layout for recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

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
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost myPost = ds.getValue(ModelPost.class);

                    // add to list
                    postList.add(myPost);

                    // adapter
                    adapterPosts = new AdapterPosts(postList, getActivity());
                    // set this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchMyPosts(final String searchQuery) {
        // linear layout for recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());

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
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost myPost = ds.getValue(ModelPost.class);

                    if (myPost.getPostTitle().toLowerCase().contains(searchQuery.toLowerCase()) || myPost.getPostDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                        // add to list
                        postList.add(myPost);

                    }

                    // adapter
                    adapterPosts = new AdapterPosts(postList, getActivity());
                    // set this adapter to recycler view
                    postsRecyclerView.setAdapter(adapterPosts);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkStoragePermission() {
        // check is storage permission is enabled or not
        // return true if enabled
        // return false if not enabled

        boolean result = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        // request runtime storage permission
        requestPermissions(storagePermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        // check is camera permission is enabled or not
        // return true if enabled
        // return false if not enabled

        boolean cameraPermission = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean cameraStoragePermission = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraPermission && cameraStoragePermission;
    }

    private void requestCameraPermission() {
        // request runtime camera permission
        requestPermissions(cameraPermission, STORAGE_REQUEST_CODE);
    }

    private void showEditProfileDialog() {
        /* show dialog containing options
         * Edit profile picture
         * Edit cover photo
         * Edit name
         * Edit phone
         * */

        // option to show in dialog
        String[] options = {"Edit profile picture", "Edit cover photo", "Edit name", "Edit phone"};

        // alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // set title
        builder.setTitle("Choose Action");

        // set items to display
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // handle dialog items clicks
                if (which == 0) {
                    // Edit profile clicked
                    progressDialog.setMessage("Updating Profile Picture..");
                    profileOrCoverPhoto = "image"; // make sure assign value
                    showImagePickerDialog();

                } else if (which == 1) {
                    // Edit cover clicked
                    progressDialog.setMessage("Updating cover photo..");
                    profileOrCoverPhoto = "cover"; // make sure assign value
                    showImagePickerDialog();

                } else if (which == 2) {
                    // Edit name clicked
                    progressDialog.setMessage("Updating Name..");
                    // calling method and pass key "name" as parameter to update it'AdapterPosts value in database
                    showNamePhoneUpdateDialog("name");

                } else if (which == 3) {
                    // Edit phone clicked
                    progressDialog.setMessage("Updating Phone..");
                    showNamePhoneUpdateDialog("phone");
                }
            }
        });
        // create and show dialog
        builder.create().show();
    }

    private void showNamePhoneUpdateDialog(final String keyName) {
        /* parameter "keyName" will contain value :
         * "name" which is key in user'AdapterPosts database which is used to update user.
         * "phone" which is key in user'AdapterPosts database which is used to update phone.
         * */

        // custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update " + keyName); // e.g Update name or Update phone

        // set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10, 10, 10, 10);

        // add edit text
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter " + keyName); // Edit name or Edit phone
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        // add positive buttons in dialog to update
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // input text from edit text
                final String value = editText.getText().toString().trim();

                // validate if user has entered something or not
                if (!TextUtils.isEmpty(value)) {

                    progressDialog.show();
                    HashMap<String, Object> result = new HashMap<>();
                    result.put(keyName, value);

                    databaseReference.child(firebaseUser.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    // updated, dismiss progress
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(), "Updated...", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // failed , dismiss progress,get and show error message
                            progressDialog.dismiss();
                            Toast.makeText(getActivity(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    // if user edit his name, also change it from his posts

                    if (keyName.equals("name")) {
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = databaseReference.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    String child = ds.getKey();
                                    assert child != null;
                                    dataSnapshot.getRef().child(child).child("userName").setValue(value);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(getActivity(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(getActivity(), "Please enter " + keyName, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // add negative buttons in dialog to cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        // create and show dialog
        builder.create().show();
    }

    private void showImagePickerDialog() {
        // show dialog containing options camera and gallery to pick the image
        // option to show in dialog
        String[] options = {"Camera", "Gallery"};

        // alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // set title
        builder.setTitle("Pick an Image From");

        // set items to display
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // handle dialog items clicks
                if (which == 0) {
                    // Camera clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }
                } else if (which == 1) {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /*
         * Thi method called when user press allow or deny from permission request dialog
         * hre we will handle permission cases (allowed & denied)
         * */

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                // picking from camera,first check if camera and storage permission allowed or not
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        // permission enabled
                        pickFromCamera();
                    } else {
                        // permissions denied
                        Toast.makeText(getActivity(), "Please enable camera and storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                // picking from gallery,first check if storage permission allowed or not
                if (grantResults.length > 0) {

                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        // permission enabled
                        pickFromGallery();
                    } else {
                        // permissions denied
                        Toast.makeText(getActivity(), "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        /* This method will be called after picking image from Camera or Gallery*/
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {

                // image is picked from gallery,get uri of image
                assert data != null;
                imageUri = data.getData();
                uploadFromFileCoverPhoto(imageUri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // image is picked from camera,get uri og image
                uploadFromFileCoverPhoto(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadFromFileCoverPhoto(final Uri imageUri) {

        // show progress
        progressDialog.show();


        /*
         * Instead of containing separate function for profile picture and cover photo
         * Here doing work for both in same function
         *
         * To add check i will add a string variable and assign it value "image" when user clicks
         * "Edit profile picture" and assign it value "cover" when user clicks "Edit cover photo"
         *
         * Here! image is the key in each user containing uri of user'AdapterPosts profile picture
         *
         * cover is the key in each user containing uri of user'AdapterPosts cover photo
         * */

        /* The parameter "image uri " contains the uri of image picked either from camera or gallery
         *
         * We will use UID of the currently signed in user as name of the image so there will be only one image
         * profile and one image for cover for each user
         *
         * */

        // path and name of image to be stored in firebase storage
        // e.g Users_Profile_Cover_Images/image_e12f3876.jpg
        // e.g Users_Profile_Cover_Images/cover_e12f3876.jpg

        String filePathAndName = storagePath + "" + profileOrCoverPhoto + "_" + firebaseUser.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);

        storageReference2nd.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        // image is uploaded to storage,now get its url and store in user database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        final Uri downloadURI = uriTask.getResult();

                        // check if image is uploaded or not and uri is received
                        if (uriTask.isSuccessful()) {

                            // image upload
                            // add/update url in user'AdapterPosts database
                            HashMap<String, Object> results = new HashMap<>();
                            /* First Parameter is profile or Cover Photo that has value "image" or "cover"
                             * which are keys in user'AdapterPosts database where url of image will be saved in one of them
                             * Second parameter contains the url of the image stored in firebase storage ,this url will be saved as value against key "image" or "cover"
                             *
                             * */

                            assert downloadURI != null;
                            results.put(profileOrCoverPhoto, downloadURI.toString());

                            databaseReference.child(firebaseUser.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            // url in database of user is added successfully
                                            // dismiss progress bar
                                            progressDialog.dismiss();
                                            Toast.makeText(getActivity(), "Image Updated Successfully...", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    // error adding url in database of user
                                    // dismiss progress bar
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                            // if user edit his profile, also change it from his posts

                            if (profileOrCoverPhoto.equals("image")) {
                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Posts");
                                Query query = databaseReference.orderByChild("uid").equalTo(uid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                            String child = ds.getKey();
                                            assert child != null;
                                            dataSnapshot.getRef().child(child).child("userProfile").setValue(downloadURI.toString());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        } else {
                            // error
                            progressDialog.show();
                            Toast.makeText(getActivity(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                // there is some error(AdapterPosts),get and show error message,dismiss progress dialog
                progressDialog.dismiss();
                Toast.makeText(getActivity(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickFromCamera() {

        // intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        // put image uri
        imageUri = Objects.requireNonNull(getActivity()).getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        // pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void checkUserStatus() {
        // get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // user is signed in stay here
            // set email of logged in user
            // emailText.setText(user.getEmail());
            uid = user.getUid();
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

        MenuItem item = menu.findItem(R.id.actionSearch);
        // search view of search user specific post
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // called when user press search button
                if (!TextUtils.isEmpty(query)) {
                    // search
                    searchMyPosts(query);
                } else {
                    loadMyPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // called whenever user type any letter
                if (!TextUtils.isEmpty(newText)) {
                    // search
                    searchMyPosts(newText);
                } else {
                    loadMyPosts();
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
