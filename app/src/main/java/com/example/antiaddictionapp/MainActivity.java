package com.example.antiaddictionapp;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int VPN_REQUEST_CODE = 0x0F;
    private boolean isProtectionOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button toggleButton = findViewById(R.id.toggleButton);
        TextView statusText = findViewById(R.id.statusText);

        // Hide the button to prevent the user from stopping it easily
        toggleButton.setVisibility(android.view.View.GONE);

        // Automatically prompt for VPN permission on app start
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE);
        } else {
            // Already prepared, start service
            startVpn();
        }
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        TextView statusText = findViewById(R.id.statusText);
        statusText.setText("Protection is Permanently ON");
        isProtectionOn = true;
    }

    private void stopVpn() {
        // Stop functionality disabled for permanent protection
        // Intent intent = new Intent(this, LocalVpnService.class);
        // stopService(intent);
    }
}
