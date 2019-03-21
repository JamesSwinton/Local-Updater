package com.zebra.jamesswinton.localupdater;

import static com.zebra.jamesswinton.localupdater.App.UpdateType.DOWNGRADE;
import static com.zebra.jamesswinton.localupdater.App.UpdateType.UPGRADE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.zebra.jamesswinton.localupdater.App.UpdateType;
import com.zebra.jamesswinton.localupdater.AsyncUpdaters.AsyncCopier;
import com.zebra.jamesswinton.localupdater.AsyncUpdaters.AsyncUpdater;
import com.zebra.jamesswinton.localupdater.AsyncUpdaters.ManualAsyncUpdater;
import com.zebra.jamesswinton.localupdater.AsyncUpdaters.UplAsyncUpdater;
import com.zebra.jamesswinton.localupdater.BroadcastReceivers.CopyIntentReceiver;
import com.zebra.jamesswinton.localupdater.BroadcastReceivers.DowngradeIntentReceiver;
import com.zebra.jamesswinton.localupdater.BroadcastReceivers.UpdateIntentReceiver;
import com.zebra.jamesswinton.localupdater.Interfaces.UpdateProgressInterface;
import com.zebra.jamesswinton.localupdater.databinding.ActivityMainBinding;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  // Debugging
  private static final String TAG = "MainActivity";

  // Constants
  private static final int PERMISSION_ALL = 100;
  private static final String[] PERMISSIONS = {
      android.Manifest.permission.WRITE_EXTERNAL_STORAGE
  };

  private static final String COPY_INTENT_ACTION = "com.zebra.COPYFILE";
  private static final String UPDATE_INTENT_ACTION = "com.zebra.UPDATE";
  private static final String DOWNGRADE_INTENT_ACTION = "com.zebra.DOWNGRADE";

  private static final Handler mHandler = new Handler(Looper.getMainLooper());

  // Static Variables
  private static UpdateType mUpdateType;
  private static File mLocalUpdatePackage;
  private static AlertDialog mUpdateProgressDialog;

  private static IntentFilter mCopyIntentFilter;
  private static IntentFilter mUpgradeIntentFilter;
  private static IntentFilter mDowngradeIntentFilter;
  private static CopyIntentReceiver mCopyIntentReceiver;
  private static UpdateIntentReceiver mUpdateIntentReceiver;
  private static DowngradeIntentReceiver mDowngradeIntentReceiver;

  // Non-Static Variables
  private ActivityMainBinding mDataBinding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Init DataBinding
    mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    // Get Permissions
    getPermissions();

    // Init UI
    initUI();

    // Init Click Listeners
    initClickListeners();

    // Init Receivers
    initReceivers();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Unregister Copy Receiver
    if (mCopyIntentReceiver != null) {
      unregisterReceiver(mCopyIntentReceiver);
    }

    // Unregister Update Receiver
    if (mUpdateIntentReceiver != null) {
      unregisterReceiver(mUpdateIntentReceiver);
    }

    // Unregister Downgrade Receiver
    if (mDowngradeIntentReceiver != null) {
      unregisterReceiver(mDowngradeIntentReceiver);
    }
  }

  public static void startCopyFromIntent(Context context, @NonNull String packageName) {
    new AsyncCopier(packageName, new UpdateProgressInterface() {
      @Override
      public void onStart() {
        // Create Progress Dialog
        mUpdateProgressDialog = new AlertDialog.Builder(context)
            .setTitle("Performing File Copy")
            .setMessage("Starting Copy...")
            .setCancelable(false)
            .create();

        // Show Dialog
        mUpdateProgressDialog.show();
      }

      @Override
      public void onProgress(String progressText) {
        mHandler.post(() -> mUpdateProgressDialog.setMessage(progressText));
      }

      @Override
      public void onFinish(@Nullable List<File> localUpdatePackages) {
        // Dismiss dialog
        mHandler.post(() -> {
          mUpdateProgressDialog.setMessage("File Copied Successfully");
          mUpdateProgressDialog.setCancelable(true);
          mUpdateProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
              null, (dialog, which) -> dialog.dismiss());
        });
      }

      @Override
      public void onError(String error) {
        mHandler.post(() -> {
          mUpdateProgressDialog.setMessage(error);
          mUpdateProgressDialog.setCancelable(true);
          mUpdateProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK",
              null, (dialog, which) -> dialog.dismiss());
        });
      }
    }).execute();
  }

  public static void startUpdateFromIntent(Context context, @NonNull String updatePackageName,
      UpdateType updateType) {
    new AsyncUpdater(updatePackageName, updateType, new UpdateProgressInterface() {
      @Override
      public void onStart() {
        // Create Progress Dialog
        mUpdateProgressDialog = new AlertDialog.Builder(context)
            .setTitle("Performing Update")
            .setMessage("Starting Update...")
            .setCancelable(false)
            .create();

        // Show Dialog
        mUpdateProgressDialog.show();
      }

      @Override
      public void onProgress(String progressText) {
        mHandler.post(() -> mUpdateProgressDialog.setMessage(progressText));
      }

      @Override
      public void onFinish(@Nullable List<File> localUpdatePackages) {
        // Dismiss dialog
        mHandler.post(() -> mUpdateProgressDialog.dismiss());
      }

      @Override
      public void onError(String error) {
        mHandler.post(() -> {
          mUpdateProgressDialog.setMessage(error);
          mUpdateProgressDialog.setCancelable(true);
          mUpdateProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK",
              null, null);
        });
      }
    }).execute();
  }

  public static void startUplUpdateFromIntent(Context context, @NonNull List<String> updatePackageNames,
      UpdateType updateType) {
    new UplAsyncUpdater(updatePackageNames, updateType, new UpdateProgressInterface() {
      @Override
      public void onStart() {
        // Create Progress Dialog
        mUpdateProgressDialog = new AlertDialog.Builder(context)
            .setTitle("Performing Update")
            .setMessage("Starting Update...")
            .setCancelable(false)
            .create();

        // Show Dialog
        mUpdateProgressDialog.show();
      }

      @Override
      public void onProgress(String progressText) {
        mHandler.post(() -> mUpdateProgressDialog.setMessage(progressText));
      }

      @Override
      public void onFinish(@Nullable List<File> localUpdatePackages) {
        // Dismiss dialog
        mHandler.post(() -> mUpdateProgressDialog.dismiss());
      }

      @Override
      public void onError(String error) {
        mHandler.post(() -> {
          mUpdateProgressDialog.setMessage(error);
          mUpdateProgressDialog.setCancelable(true);
          mUpdateProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK",
              null, null);
        });
      }
    }).execute();
  }

  public static void startUpdateFromButtonPress(Context context) {
    new ManualAsyncUpdater(new UpdateProgressInterface() {
      @Override
      public void onStart() {
        // Create Progress Dialog
        mUpdateProgressDialog = new AlertDialog.Builder(context)
            .setTitle(mUpdateType == UPGRADE ? "Performing Update" : "Performing Downgrade")
            .setMessage("Starting Update...")
            .setCancelable(false)
            .create();

        // Show Dialog
        mUpdateProgressDialog.show();
      }

      @Override
      public void onProgress(String progressText) {
        mHandler.post(() -> mUpdateProgressDialog.setMessage(progressText));
      }

      @Override
      public void onFinish(@Nullable List<File> localUpdatePackages) {
        mHandler.post(() -> {
          // Dismiss dialog
          mUpdateProgressDialog.dismiss();

          // Extract names from List
          String[] localUpdatePackageNames = new String[localUpdatePackages.size()];
          for (int i = 0; i < localUpdatePackages.size(); i++) {
            localUpdatePackageNames[i] = localUpdatePackages.get(i).getName();
          }

          // Show new Dialog with Option to Select File
          AlertDialog selectUpdatePackage = new AlertDialog.Builder(context)
              .setTitle("Select Update Package")
              .setSingleChoiceItems(localUpdatePackageNames, -1, (dialog, item) -> {
                // Store Chosen File
                for (File updatePackage : localUpdatePackages) {
                  if (updatePackage.getName().equals(localUpdatePackageNames[item])) {
                    mLocalUpdatePackage = updatePackage;
                  }
                }
                dialog.dismiss();
              })
              .setOnDismissListener(dialog -> startUpdateViaProfileManager(context))
              .setCancelable(false)
              .create();

          // Show Dialog
          selectUpdatePackage.show();
        });
      }

      @Override
      public void onError(String error) {
        mUpdateProgressDialog.setMessage(error);
        mUpdateProgressDialog.setCancelable(true);
        mUpdateProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK",
            null, null);
      }
    }).execute();
  }

  private static void startUpdateViaProfileManager(Context context) {
    // Build Profile
    String[] profile = new String[1];
    switch (mUpdateType) {
      case UPGRADE:
        profile[0] = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "  <characteristic type=\"Profile\">\n" +
            "    <parm name=\"ProfileName\" value=\"update\"/>\n" +
            "    <characteristic type=\"PowerMgr\" version=\"4.2\">\n" +
            "      <parm name=\"ResetAction\" value=\"8\"/>\n" +
            "      <characteristic type=\"file-details\">\n" +
            "        <parm name=\"ZipFile\" value=\"" + mLocalUpdatePackage.getAbsolutePath() + "\"/>\n" +
            "      </characteristic>\n" +
            "    </characteristic>\n" +
            "  </characteristic>\n";
        break;
      case DOWNGRADE:
        profile[0] = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "  <characteristic type=\"Profile\">\n" +
            "    <parm name=\"ProfileName\" value=\"update\"/>\n" +
            "    <characteristic version=\"8.1\" type=\"PowerMgr\">\n" +
            "      <parm name=\"ResetAction\" value=\"11\" />\n" +
            "      <characteristic type=\"file-details\">\n" +
            "        <parm name=\"ZipFile\" value=\"" + mLocalUpdatePackage.getAbsolutePath() + "\"/>\n" +
            "      </characteristic>\n" +
            "    </characteristic>\n" +
            "  </characteristic> ";
        break;
    }

    // Start Profile Manager
    new ProcessProfile(context).execute(profile);
  }

  private static class ProcessProfile extends AsyncTask<String, Void, EMDKResults> {

    private Context context;
    private AlertDialog startingUpdateDialog;

    ProcessProfile(Context context) {
      this.context = context;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();

      startingUpdateDialog = new AlertDialog.Builder(context)
          .setTitle("Performing Update")
          .setMessage("Performing Update via Power Manager")
          .setCancelable(false)
          .create();

      startingUpdateDialog.show();
    }

    @Override
    protected EMDKResults doInBackground(String... params) {
      // Execute Profile
      return App.mProfileManager.processProfile("update", ProfileManager.PROFILE_FLAG.SET, params);
    }

    @Override
    protected void onPostExecute(EMDKResults results) {
      super.onPostExecute(results);

      // Log Result
      Log.i(TAG, "Profile Manager Result: " + results.statusCode + " | " + results.extendedStatusCode);

      // Update Dialog
      startingUpdateDialog.setMessage("Update Triggered...");

      //Check the return status of processProfile
      switch (results.statusCode) {
        case FAILURE:
        case UNKNOWN:
        case NULL_POINTER:
        case EMPTY_PROFILENAME:
        case EMDK_NOT_OPENED:
        case PREVIOUS_REQUEST_IN_PROGRESS:
        case PROCESSING:
        case NO_DATA_LISTENER:
        case FEATURE_NOT_READY_TO_USE:
        case FEATURE_NOT_SUPPORTED:
          startingUpdateDialog.setMessage("Update Failed. Please try again.");
          startingUpdateDialog.setCancelable(true);
          break;
      }
    }
  }

  private void initUI() {
    // Init Version Identifier
    mDataBinding.versionText.setText(BuildConfig.VERSION_NAME);
    // Get Status Icon
    Drawable statusIcon = getDrawable(App.mEmdkState.isRunning()
        ? R.drawable.ic_success : R.drawable.ic_error);
    // Init Status Icon
    mDataBinding.emdkStatusIcon.setImageDrawable(statusIcon);
    // Set Listener on EMDK State
    App.mEmdkState.setListener((boolean isRunning) -> mDataBinding.emdkStatusIcon.setImageDrawable(
        getDrawable(isRunning ? R.drawable.ic_success : R.drawable.ic_error)));
  }

  private void initClickListeners() {
    mDataBinding.upgradeContainer.setOnClickListener(v -> {
      mUpdateType = UpdateType.UPGRADE;
      startUpdateFromButtonPress(this);
    });

    mDataBinding.downgradeContainer.setOnClickListener(v -> {
      mUpdateType = UpdateType.DOWNGRADE;
      startUpdateFromButtonPress(this);
    });

  }

  private void initReceivers() {
    // Create Receivers
    if (mCopyIntentReceiver == null) { mCopyIntentReceiver = new CopyIntentReceiver(); }
    if (mUpdateIntentReceiver == null) { mUpdateIntentReceiver = new UpdateIntentReceiver(); }
    if (mDowngradeIntentReceiver == null) { mDowngradeIntentReceiver = new DowngradeIntentReceiver(); }

    // Create Filters
    if (mCopyIntentFilter == null) { mCopyIntentFilter = new IntentFilter(); }
    if (mUpgradeIntentFilter == null) { mUpgradeIntentFilter = new IntentFilter(); }
    if (mDowngradeIntentFilter == null) { mDowngradeIntentFilter = new IntentFilter(); }

    // Add Actions
    mCopyIntentFilter.addAction(COPY_INTENT_ACTION);
    mUpgradeIntentFilter.addAction(UPDATE_INTENT_ACTION);
    mDowngradeIntentFilter.addAction(DOWNGRADE_INTENT_ACTION);

    // Register Receivers
    registerReceiver(mCopyIntentReceiver, mCopyIntentFilter);
    registerReceiver(mUpdateIntentReceiver, mUpgradeIntentFilter);
    registerReceiver(mDowngradeIntentReceiver, mDowngradeIntentFilter);
  }

  private void getPermissions() {
    if(!hasPermissions(this, PERMISSIONS)) {
      ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
    }
  }

  public static boolean hasPermissions(Context context, String... permissions) {
    if (context != null && permissions != null) {
      for (String permission : permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
      @NonNull int[] grantResults) {
    switch (requestCode) {
      case PERMISSION_ALL: {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          getPermissions();
        }
      }
    }
  }
}
