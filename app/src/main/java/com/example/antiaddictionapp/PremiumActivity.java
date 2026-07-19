package com.example.antiaddictionapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class PremiumActivity extends AppCompatActivity {

    private static final String SECRET_CODE = "UNLOCK-PRO-2026";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        Button btnBuyCode = findViewById(R.id.btnBuyCode);
        Button btnVerifyCode = findViewById(R.id.btnVerifyCode);
        EditText etActivationCode = findViewById(R.id.etActivationCode);

        // This will open Gumroad/Patreon link later.
        btnBuyCode.setOnClickListener(v -> {
            // Toast for now until they have a URL
            Toast.makeText(this, "Redirecting to purchase page...", Toast.LENGTH_SHORT).show();
            // Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://your-gumroad-link.com"));
            // startActivity(browserIntent);
        });

        btnVerifyCode.setOnClickListener(v -> {
            String enteredCode = etActivationCode.getText().toString().trim();
            if (enteredCode.equalsIgnoreCase(SECRET_CODE)) {
                try {
                    MasterKey masterKey = new MasterKey.Builder(this)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build();

                    SharedPreferences prefs = EncryptedSharedPreferences.create(
                            this,
                            "SecureAppPrefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );
                    prefs.edit().putBoolean("isPremium", true).apply();
                    
                    Toast.makeText(this, "PRO Unlocked! Ads are now disabled.", Toast.LENGTH_LONG).show();
                    finish(); // Close activity and return to Main
                } catch (Exception e) {
                    Toast.makeText(this, "Security Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Invalid Activation Code", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
