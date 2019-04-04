package com.example.gates.facetoface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class ActivityCodeVerification extends Activity {
    private String code;
    private EditText codeInputEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_verification);
        codeInputEditText = findViewById(R.id.code_input);

        final EditText codeInput =  findViewById(R.id.code_input);
        final Button codeInputSubmit = findViewById(R.id.verify_code);

        codeInputSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = codeInput.getText().toString();
                Intent intent = new Intent();
                Log.d(">>>>", "entered code" + code);
                intent.putExtra("code", "" + code);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

}
