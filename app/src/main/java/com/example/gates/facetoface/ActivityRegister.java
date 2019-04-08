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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;


public class ActivityRegister extends Activity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int GET_FROM_GALLERY = 2;

    private EditText nameInput;
    private Button registerButton;
    private ImageView imageView;
    private Bitmap bitmap;

    private boolean pictureChanged = false;

    private Uri uri;
    private StorageReference imageRef;
    private String name;

    @Override
    public void onBackPressed() {

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getActionBar().setTitle("Register");
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        imageView = findViewById(R.id.new_profile_picture);
        nameInput = findViewById(R.id.name_input);
        registerButton = findViewById(R.id.register_button);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                giveOption();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                validateAccount();
            }
        });
    }
    private void giveOption(){
        //gives option between taking picture and uploading picture
        AlertDialog.Builder builder1 = new AlertDialog.Builder(ActivityRegister.this);
        final AlertDialog alert1 = builder1.create();

        LinearLayout layout = new LinearLayout(ActivityRegister.this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        Button takePicture = new Button(ActivityRegister.this);
        takePicture.setText("Take picture");
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
                alert1.dismiss();
            }
        });
        Button uploadPicture = new Button(ActivityRegister.this);
        uploadPicture.setText("Upload picture");
        uploadPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPicture();
                alert1.dismiss();
            }
        });
        layout.addView(takePicture);
        layout.addView(uploadPicture);
        alert1.setView(layout);
        alert1.setTitle("New profile picture");
        alert1.show();

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
    private void validateAccount(){
        //make sure picture is valid
        if(!pictureChanged){
            Toast.makeText(getApplicationContext(), "You must add a picture", Toast.LENGTH_SHORT).show();
            return;
        }
        //make sure name is valid
        name = nameInput.getText().toString().trim();
        if(name.equals("")){
            Toast.makeText(getApplicationContext(), "You must enter a name", Toast.LENGTH_SHORT).show();
            return;
        }
        createAccount();
    }
    private void createAccount(){
        //enter details into firebase
        DatabaseReference databaseUsers = FirebaseDatabase.getInstance().getReference().child("users");
        FirebaseUser user =  FirebaseAuth.getInstance().getCurrentUser();
        String id = user.getUid();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        User newUser = new User(user);
        newUser.setName(name);
        newUser.setImageB64(encoded);
        databaseUsers.child(id).setValue(newUser);
        finish();
    }
    private void resizeBitmap(){
        float aspectRatio = bitmap.getWidth() /
                (float) bitmap.getHeight();
        int width = 800;
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
            pictureChanged = true;
        }
        else if (requestCode == GET_FROM_GALLERY && resultCode == RESULT_OK) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                resizeBitmap();
                imageView.setImageBitmap(bitmap);
                pictureChanged = true;
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
