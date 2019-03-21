package com.zebra.jamesswinton.localupdater.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zebra.jamesswinton.localupdater.App.UpdateType;
import com.zebra.jamesswinton.localupdater.MainActivity;
import java.util.ArrayList;
import java.util.List;

public class DowngradeIntentReceiver extends BroadcastReceiver {

    // Debugging
    private static final String TAG = "IntentReceiver";

    // Constants
    private static final String PACKAGE_NAME = "package";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Downgrade Intent Received - Starting Downgrade");

        // Validate Packages Sent
        if (intent.getExtras() != null) {
            // Only one Package sent -> Start Update
            if (intent.getExtras().size() == 1) {
                String updatePackageName = intent.getStringExtra(PACKAGE_NAME);
                MainActivity.startUpdateFromIntent(context, updatePackageName, UpdateType.DOWNGRADE);
            } else {
                List<String> updatePackageNames = new ArrayList<>();
                for (int i = 0; i < intent.getExtras().size(); i++) {
                    updatePackageNames.add(intent.getStringExtra(PACKAGE_NAME + "-" + i));
                }
                MainActivity.startUplUpdateFromIntent(context, updatePackageNames, UpdateType.DOWNGRADE);
            }
        }
    }
}
