package com.example.antiaddictionapp;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class AppLockerService extends AccessibilityService {

    // Hardcoded for now based on user request.
    // In a full implementation, these would be retrieved from SharedPreferences
    private static final String INSTAGRAM_PKG = "com.instagram.android";
    private static final String YOUTUBE_PKG = "com.google.android.youtube";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String packageName = event.getPackageName().toString();
                
                // If they open Instagram or YouTube
                if (packageName.equals(INSTAGRAM_PKG) || packageName.equals(YOUTUBE_PKG)) {
                    // Logic to check timer could go here
                    // For now, immediately block
                    showBlockScreen(packageName);
                }
            }
        }
    }

    private void showBlockScreen(String packageName) {
        Intent intent = new Intent(this, BlockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("blocked_app", packageName);
        startActivity(intent);
    }

    @Override
    public void onInterrupt() {
        // Required, but we don't need to do anything here
    }
}
