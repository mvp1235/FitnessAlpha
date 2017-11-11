package com.example.mvp.fitnessalpha;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import static com.example.mvp.fitnessalpha.MainScreenActivity.MY_PREFERENCE;

public class EditUserInfoActivity extends AppCompatActivity {

    private EditText userNameET, weightET;
    private Spinner genderSpinner;
    private Button saveProfileBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_info);

        Intent intent = getIntent();

        userNameET = (EditText) findViewById(R.id.editUserNameET);
        genderSpinner = (Spinner) findViewById(R.id.genderSpinner);
        weightET = (EditText) findViewById(R.id.editWeightValue);
        saveProfileBtn = (Button) findViewById(R.id.saveProfileBtn);


        String currentName = intent.getStringExtra(UserTable.NAME);
        String currentGender = intent.getStringExtra(UserTable.GENDER);
        String currentWeight = intent.getStringExtra(UserTable.WEIGHT);

        userNameET.setText(currentName);
        userNameET.setSelection(userNameET.getText().length()); // move cursor to the end at start
        weightET.setText(currentWeight);

        if (currentGender.equalsIgnoreCase("Male")) {
            genderSpinner.setSelection(0);
        } else {
            genderSpinner.setSelection(1);
        }

        saveProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfile();
            }
        });
    }

    public void saveProfile() {
        //Update user database
        ContentValues contentValues = new ContentValues();
        contentValues.put(UserTable.NAME, userNameET.getText().toString());
        contentValues.put(UserTable.GENDER, genderSpinner.getSelectedItem().toString());
        contentValues.put(UserTable.WEIGHT, weightET.getText().toString());
        getContentResolver().update(MyContentProvider.CONTENT_URI, contentValues, "_id = ?", new String[] {"1"});

        //Send back data to update profile screen
        Intent returnIntent = new Intent();
        returnIntent.putExtra("name", userNameET.getText().toString());
        returnIntent.putExtra("weight", weightET.getText().toString());
        returnIntent.putExtra("gender", genderSpinner.getSelectedItem().toString());
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}
