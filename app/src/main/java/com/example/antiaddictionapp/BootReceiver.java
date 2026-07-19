package com.example.antiaddictionapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Device booted, attempting to start VPN service");
            
            // Check if VPN is already prepared
            Intent vpnIntent = VpnService.prepare(context);
            if (vpnIntent == null) {
                // VPN is prepared, we can start it directly
                Intent startIntent = new Intent(context, LocalVpnService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(startIntent);
                } else {
                    context.startService(startIntent);
                }
            } else {
                Log.w(TAG, "VPN is not prepared. User needs to open the app first to grant permissions.");
                // If the user hasn't granted permission, we can't start the VPN from a receiver.
                // The user must open the app.
            }
        }
    }
}
