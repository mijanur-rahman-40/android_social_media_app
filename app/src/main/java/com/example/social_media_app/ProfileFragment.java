package com.example.social_media_app;


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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    // firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    // views from xml
    private ImageView avatarImageView, coverImageView;
    private TextView nameText, emailText, phoneText;

    private FloatingActionButton floatingActionButton;

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

    // uri of picked image
    private Uri imageUri;

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

                    // set data
                    nameText.setText(name);
                    emailText.setText(email);
                    phoneText.setText(phone);

                    try {
                        // if image is received then set
                        Picasso.get().load(image).into(avatarImageView);
                    } catch (Exception e) {
                        // if there is any exception while setting image then set default
                        Picasso.get().load(R.drawable.ic_action_add_image).into(avatarImageView);
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
        return view;
    }

    private boolean checkStoragePermission(){
        // check is storage permission is enabled or not
        // return true if enabled
        // return false if not enabled

        boolean result = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()),Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result;
    }

    private void requestStoragePermission(){
        // request runtime storage permission
        ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),storagePermission,CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        // check is camera permission is enabled or not
        // return true if enabled
        // return false if not enabled

        boolean cameraPermission = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()),Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean cameraStoragePermission = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()),Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return  cameraPermission && cameraStoragePermission;
    }

    private void requestCameraPermission(){
        // request runtime camera permission
        ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),cameraPermission,STORAGE_REQUEST_CODE);
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
                    progressDialog.setMessage("Updating Profile Picture");
                    showImagePickerDialog();
                } else if (which == 1) {
                    // Edit cover clicked
                    progressDialog.setMessage("Updating cover Photo");
                } else if (which == 2) {
                    // Edit name clicked
                    progressDialog.setMessage("Updating Name");
                } else if (which == 3) {
                    // Edit phone clicked
                    progressDialog.setMessage("Updating Phone");
                }
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
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }else {
                        pickFromCamera();
                    }
                } else if (which == 1) {
                    // gallery clicked
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }else {
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

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                // picking from camera,first check if camera and storage permission allowed or not
                if (grantResults.length >0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        // permission enabled
                        pickFromCamera();
                    }else {
                        // permissions denied
                        Toast.makeText(getActivity(), "Please enable camera and storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            case STORAGE_REQUEST_CODE:{
                // picking from gallery,first check if storage permission allowed or not
                if (grantResults.length >0){

                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        // permission enabled
                        pickFromGallery();
                    }else {
                        // permissions denied
                        Toast.makeText(getActivity(), "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        /* This method will be called after picking image from Camera or Gallery*/
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                // image is picked from gallery,get uri of image
                imageUri = data.getData();
                uploadFromFileCoverPhoto(imageUri);
            }if (requestCode == IMAGE_PICK_CAMERA_CODE){
                // image is picked from camera,get uri og image
                uploadFromFileCoverPhoto(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadFromFileCoverPhoto(Uri imageUri) {
        /*
        * Instead of containing separate function for profile picture and cover photo
        * Here doing work for both in same function
        *
        * To add check i will add a string variable and assign it value "image" when user clicks
        * "Edit profile picture" and assign it value "cover" when user clicks "Edit cover photo"
        *
        * Here! image is the key in each user containing uri of user's profile picture
        *
        * cover is the key in each user containing uri of userr's cover photo
        * */
    }

    private void pickFromCamera() {
        // intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");
        // put image uri
        imageUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        // intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        // pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);
    }
}
