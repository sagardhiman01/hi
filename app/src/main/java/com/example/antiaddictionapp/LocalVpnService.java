package com.example.antiaddictionapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class LocalVpnService extends VpnService {

    private static final String TAG = "LocalVpnService";
    private ParcelFileDescriptor vpnInterface;
    private FilteringEngine filteringEngine;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "VPN_CHANNEL_ID")
                .setContentTitle("AntiAddiction Protection")
                .setContentText("Your protection is permanently active.")
                .setSmallIcon(android.R.drawable.ic_secure)
                .setOngoing(true)
                .build();
        startForeground(1, notification);

        if (filteringEngine == null) {
            filteringEngine = new FilteringEngine();
        }
        setupVpn();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "VPN_CHANNEL_ID",
                    "VPN Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void setupVpn() {
        if (vpnInterface != null) {
            return;
        }
        
        Builder builder = new Builder();
        builder.addAddress("10.0.0.2", 32);
        builder.addRoute("0.0.0.0", 0);
        
        // This sets up the DNS interceptor by defining a virtual DNS server
        builder.addDnsServer("10.0.0.3"); 
        
        // Exclude the app itself so we can fetch blocklist updates if needed
        try {
            builder.addDisallowedApplication(getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to exclude app from VPN", e);
        }

        try {
            vpnInterface = builder.establish();
            Log.i(TAG, "VPN interface established");
            
            // In a complete implementation, a thread would be started here to read
            // from vpnInterface.getFileDescriptor(), parse the IP packets, identify
            // UDP port 53 (DNS), and pass the requested domains to FilteringEngine.
            // If FilteringEngine.shouldBlock(domain) returns true, the DNS request
            // is answered with a sinkhole IP. Otherwise, it is forwarded.
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to establish VPN", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close VPN interface", e);
            }
            vpnInterface = null;
        }
    }
}
