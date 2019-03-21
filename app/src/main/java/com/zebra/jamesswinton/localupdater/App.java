package com.zebra.jamesswinton.localupdater;

import android.app.Application;
import android.util.Log;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKResults.STATUS_CODE;
import com.symbol.emdk.ProfileManager;

public class App extends Application implements EMDKListener {

  // Debugging
  public static final boolean DEBUGGING = false;
  private static final String TAG = "ApplicationClass";

  // Constants
  public enum UpdateType { UPGRADE, DOWNGRADE, COPY_ONLY }

  // Static Variables
  public static EMDKState mEmdkState = new EMDKState();

  // Non-Static Variables
  private static EMDKManager mEmdkManager;
  public static ProfileManager mProfileManager;

  @Override
  public void onCreate() {
    super.onCreate();

    // Init EMDK
    EMDKResults emdkManagerResults = EMDKManager.getEMDKManager(this, this);

    // Verify EMDK Manager
    if (emdkManagerResults == null || emdkManagerResults.statusCode != STATUS_CODE.SUCCESS) {
      // Log Error
      Log.e(TAG, "onCreate: Failed to get EMDK Manager -> " + emdkManagerResults.statusCode);
      // Update Variable
      mEmdkState.setRunning(false);
    }
  }

  /**
   * EMDK Manager Callback - Fired when EMDK Manager is available.
   * @param emdkManager -> EMDK Manager Instance
   */
  @Override
  public void onOpened(EMDKManager emdkManager) {
    // Assign EMDK Reference
    mEmdkManager = emdkManager;

    // Get Profile & Version Manager Instances
    mProfileManager = (ProfileManager) mEmdkManager.getInstance(FEATURE_TYPE.PROFILE);

    // Update Holder Variables
    mEmdkState.setRunning(mProfileManager != null);

    // Log Null Instance
    if (!mEmdkState.isRunning()) {
      // Log Error
      Log.e(TAG, "onOpened: Profile Manager Instance is Null");
    }
  }

  /**
   * EMDK Manage Callback - Fired when EMDK Manager is no longer available
   */
  @Override
  public void onClosed() {
    // Release EMDK Manager Instance
    if (mEmdkManager != null) {
      mEmdkState.setRunning(false);
      mEmdkManager.release();
      mEmdkManager = null;
    }
  }
}
