package com.example.antiaddictionapp;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 0x0F;
    private static final int DEVICE_ADMIN_REQUEST_CODE = 0x10;
    
    private ComponentName componentName;
    private DevicePolicyManager devicePolicyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, AdminReceiver.class);

        Button btnEnableAdmin = findViewById(R.id.btnEnableAdmin);
        Button btnSetupAppLocker = findViewById(R.id.btnSetupAppLocker);
        
        btnEnableAdmin.setOnClickListener(v -> enableDeviceAdmin());
        btnSetupAppLocker.setOnClickListener(v -> {
            Intent accessibilityIntent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accessibilityIntent);
            Toast.makeText(this, "Please enable Anti-Addiction Shield in Accessibility Services", Toast.LENGTH_LONG).show();
        });

        // Automatically prompt for VPN permission on app start
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            startVpn();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Button btnEnableAdmin = findViewById(R.id.btnEnableAdmin);
        if (devicePolicyManager.isAdminActive(componentName)) {
            btnEnableAdmin.setText("Uninstall Protection is Active");
            btnEnableAdmin.setEnabled(false);
        } else {
            btnEnableAdmin.setText("Enable Uninstall Protection");
            btnEnableAdmin.setEnabled(true);
        }
    }

    private void enableDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enabling this prevents the app from being uninstalled easily, keeping adult content blocked permanently.");
        startActivityForResult(intent, DEVICE_ADMIN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpn();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startVpn() {
        Intent intent = new Intent(this, LocalVpnService.class);
        startService(intent);
        
        TextView statusText = findViewById(R.id.statusText);
        statusText.setText("Protection is Permanently ON");
    }
}
