package com.example.antiaddictionapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class BlockScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_screen);
    }
    
    @Override
    public void onBackPressed() {
        // Prevent going back to the blocked app
        moveTaskToBack(true);
    }
}
