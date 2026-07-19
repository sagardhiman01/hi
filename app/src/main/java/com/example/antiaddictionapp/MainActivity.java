package com.example.antiaddictionapp;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int DEVICE_ADMIN_REQUEST_CODE = 0x10;
    
    private ComponentName componentName;
    private DevicePolicyManager devicePolicyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, AdminReceiver.class);

        Button btnSetupDns = findViewById(R.id.btnSetupDns);
        Button btnEnableAdmin = findViewById(R.id.btnEnableAdmin);
        Button btnSetupAppLocker = findViewById(R.id.btnSetupAppLocker);
        
        btnSetupDns.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            try {
                startActivity(intent);
                Toast.makeText(this, "Find 'Private DNS' and set it to: adult-filter-dns.cleanbrowsing.org", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                // Fallback if ACTION_WIRELESS_SETTINGS is not available
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });

        btnEnableAdmin.setOnClickListener(v -> enableDeviceAdmin());
        
        btnSetupAppLocker.setOnClickListener(v -> {
            Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accessibilityIntent);
            Toast.makeText(this, "Please enable Anti-Addiction Shield in Accessibility Services", Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Button btnEnableAdmin = findViewById(R.id.btnEnableAdmin);
        if (devicePolicyManager.isAdminActive(componentName)) {
            btnEnableAdmin.setText("2. Uninstall Protection is Active");
            btnEnableAdmin.setEnabled(false);
        } else {
            btnEnableAdmin.setText("2. Enable Uninstall Protection");
            btnEnableAdmin.setEnabled(true);
        }
    }

    private void enableDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enabling this prevents the app from being uninstalled easily, keeping adult content blocked permanently.");
        startActivityForResult(intent, DEVICE_ADMIN_REQUEST_CODE);
    }
}
