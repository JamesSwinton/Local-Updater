package com.zebra.jamesswinton.localupdater.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zebra.jamesswinton.localupdater.MainActivity;

public class CopyIntentReceiver extends BroadcastReceiver {

    // Debugging
    private static final String TAG = "IntentReceiver";

    // Constants
    private static final String PACKAGE_NAME = "package";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Copy Intent Received - Starting Copy");

        // Validate Packages Sent
        if (intent.getExtras() != null) {
            // Only one Package sent -> Start Update
            String updatePackageName = intent.getStringExtra(PACKAGE_NAME);
            MainActivity.startCopyFromIntent(context, updatePackageName);
        }
    }
}
