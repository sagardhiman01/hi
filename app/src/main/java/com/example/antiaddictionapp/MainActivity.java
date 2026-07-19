package com.example.antiaddictionapp;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private static final int DEVICE_ADMIN_REQUEST_CODE = 0x10;
    
    private ComponentName componentName;
    private DevicePolicyManager devicePolicyManager;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isPremium = prefs.getBoolean("isPremium", false);

        mAdView = findViewById(R.id.adView);
        Button btnUpgradePro = findViewById(R.id.btnUpgradePro);

        // Initialize the Google Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {});

        if (!isPremium) {
            // Load Banner Ad
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            // Load Interstitial Ad
            loadInterstitialAd();
            
            btnUpgradePro.setOnClickListener(v -> {
                startActivity(new Intent(this, PremiumActivity.class));
            });
        } else {
            mAdView.setVisibility(View.GONE);
            btnUpgradePro.setVisibility(View.GONE);
        }

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, AdminReceiver.class);

        Button btnSetupDns = findViewById(R.id.btnSetupDns);
        Button btnEnableAdmin = findViewById(R.id.btnEnableAdmin);
        Button btnSetupAppLocker = findViewById(R.id.btnSetupAppLocker);
        
        btnSetupDns.setOnClickListener(v -> showInterstitialAd(() -> {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            try {
                startActivity(intent);
                Toast.makeText(this, "Find 'Private DNS' and set it to: adult-filter-dns.cleanbrowsing.org", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        }));

        btnEnableAdmin.setOnClickListener(v -> enableDeviceAdmin());
        
        btnSetupAppLocker.setOnClickListener(v -> showInterstitialAd(() -> {
            Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(accessibilityIntent);
            Toast.makeText(this, "Please enable Anti-Addiction Shield in Accessibility Services", Toast.LENGTH_LONG).show();
        }));
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        // Test Interstitial Ad Unit ID
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                        
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d(TAG, "The ad was dismissed.");
                                // Ad dismissed, load a new one
                                mInterstitialAd = null;
                                loadInterstitialAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                Log.d(TAG, "The ad failed to show.");
                                mInterstitialAd = null;
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d(TAG, "The ad was shown.");
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
    }

    private void showInterstitialAd(Runnable onAdClosedAction) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("isPremium", false)) {
            onAdClosedAction.run();
            return;
        }

        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "The ad was dismissed.");
                    mInterstitialAd = null;
                    loadInterstitialAd(); // Reload for next time
                    onAdClosedAction.run(); // Execute the pending action
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mInterstitialAd = null;
                    onAdClosedAction.run(); // Execute anyway if ad fails
                }
            });
            mInterstitialAd.show(this);
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.");
            onAdClosedAction.run(); // Execute action immediately if ad not loaded
        }
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
        
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("isPremium", false)) {
            if (mAdView != null) mAdView.setVisibility(View.GONE);
            Button btnUpgradePro = findViewById(R.id.btnUpgradePro);
            if (btnUpgradePro != null) btnUpgradePro.setVisibility(View.GONE);
        }
    }

    private void enableDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enabling this prevents the app from being uninstalled easily, keeping adult content blocked permanently.");
        startActivityForResult(intent, DEVICE_ADMIN_REQUEST_CODE);
    }
}
