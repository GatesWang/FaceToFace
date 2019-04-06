package com.example.gates.facetoface;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;


public class ActivityRegister extends Activity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int GET_FROM_GALLERY = 2;
    static final int PIC_CROP = 3;
    private ImageView imageView;
    private Bitmap bitmap;
    private Uri uri;
    private StorageReference imageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        imageView = findViewById(R.id.new_profile_picture);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                giveOption();
            }
        });
        /*
        Glide.with(this)
                .load("https://www.shareicon.net/data/128x128/2017/05/24/886427_camera_512x512.png")
                .into(imageView);
                */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBar().setTitle("Register");
        return super.onCreateOptionsMenu(menu);
    }
    private void giveOption(){
        //gives option between taking picture and uploading picture
        AlertDialog.Builder builder1 = new AlertDialog.Builder(ActivityRegister.this);
        LinearLayout layout = new LinearLayout(ActivityRegister.this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        Button takePicture = new Button(ActivityRegister.this);
        takePicture.setText("Take picture");
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        Button uploadPicture = new Button(ActivityRegister.this);
        uploadPicture.setText("Upload picture");
        uploadPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPicture();
            }
        });
        layout.addView(takePicture);
        layout.addView(uploadPicture);
        builder1.setView(layout);
        builder1.setTitle("New profile picture");
        builder1.show();

    }
    private void takePicture(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    private void uploadPicture(){
        Intent uploadPictureIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(uploadPictureIntent, GET_FROM_GALLERY);
    }
    private void storeImage(){
        //the image we want needs to be stored in imageView
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        imageRef = storageRef.child("images");
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.


            }
        });
    }
    private void validateAccount(){

    }
    private void createAccount(){

    }
    private void resizeBitmap(){
        float aspectRatio = bitmap.getWidth() /
                (float) bitmap.getHeight();
        int width = 480;
        int height = Math.round(width / aspectRatio);

        bitmap = Bitmap.createScaledBitmap(
                bitmap, width, height, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            bitmap = (Bitmap) extras.get("data");
            resizeBitmap();
            imageView.setImageBitmap(bitmap);
        }
        else if (requestCode == GET_FROM_GALLERY && resultCode == RESULT_OK) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                resizeBitmap();
                imageView.setImageBitmap(bitmap);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    /*
    final AlertDialog.Builder builder1 = new AlertDialog.Builder(ActivityChatList.this);
    LinearLayout layout = new LinearLayout(ActivityChatList.this);
        layout.setOrientation(LinearLayout.VERTICAL);
    final EditText nameLabel = new EditText(ActivityChatList.this);
        layout.addView(nameLabel);
        builder1.setView(layout);
        builder1.setCancelable(false);
        builder1.setTitle("Enter your name");

    final AlertDialog.Builder builder2 = new AlertDialog.Builder(ActivityChatList.this);
    LinearLayout layout2 = new LinearLayout(ActivityChatList.this);
        layout2.setOrientation(LinearLayout.VERTICAL);
        builder2.setView(layout2);
        builder2.setCancelable(false);
        builder2.setTitle("Need to enter name to continue");
        builder2.setPositiveButton("Try again", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            createDialogs();
        }
    });

        builder2.setNegativeButton("Close app", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
    });
    final AlertDialog alert2 = builder2.create();

        builder1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            //set name String
            person.setName(nameLabel.getText().toString());
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("users")
                    .child(person.getId())
                    .setValue(person);
            dialog.dismiss();
        }
    });

        builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            alert2.show();
        }
    });

        builder1.show();
        */
}
