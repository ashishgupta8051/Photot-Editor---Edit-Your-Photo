package com.edit.yourphoto;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getWindow().setStatusBarColor(Color.parseColor("#6495ED"));
        getSupportActionBar().hide();

        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(getApplicationContext(),PhotoEditorActivity.class));
                finish();
            }
        },1500);
    }
}