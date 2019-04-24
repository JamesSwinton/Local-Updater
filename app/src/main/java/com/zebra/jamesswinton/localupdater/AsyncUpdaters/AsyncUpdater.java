package com.zebra.jamesswinton.localupdater.AsyncUpdaters;

import android.os.AsyncTask;
import android.os.Environment;
import androidx.annotation.Nullable;
import android.util.Log;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.zebra.jamesswinton.localupdater.App;
import com.zebra.jamesswinton.localupdater.App.UpdateType;
import com.zebra.jamesswinton.localupdater.Interfaces.UpdateProgressInterface;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class AsyncUpdater extends AsyncTask<Void, String, Boolean> {

    // Debugging
    private static final String TAG = "AsyncUpdater";

    // Constants
    private static final String USB_DRIVE_REGEX = "(([A-Z0-9]{4})(-)([A-Z0-9]{4}))";
    private static final String TEST_DIRECTORY = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "Test Directory";
    private static final String EXTERNAL_USB_DIRECTORY = File.separator + "storage" + File.separator;
    private static final String INTERNAL_APP_DIRECTORY = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "Local Updater" + File.separator;

    // Non-static Variables


    // Static Variables
    private static String mUpdateResult;
    private static UpdateType mUpdateType;
    private static String mUpdatePackageName;
    private static UpdateProgressInterface mCallback;

    public AsyncUpdater(@Nullable String updatePackageName, UpdateType updateType,
                        UpdateProgressInterface callback) {
        // Get Package Name
        mUpdatePackageName = updatePackageName;
        // Get Update Type ENUM
        mUpdateType = updateType;
        // Assign reference to Interface
        mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        // Notify Calling Class
        mCallback.onStart();
    }

    @Override
    protected void onProgressUpdate(String... progressText) {
        super.onProgressUpdate(progressText);

        // Notify Calling Class
        mCallback.onProgress(progressText[0]);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        // Locate USB
        String externalUsbPath = locateExternalUsb();

        // Validate USB Path (Or stop)
        if (externalUsbPath == null) {
            return false;
        }

        // Locate Update Package
        String updatePackagePath = locatedUpdatePackageInUsb(externalUsbPath);

        // Validate Update Package (Or stop)
        if (updatePackagePath == null) {
            return false;
        }

        // Copy File to Local Storage
        boolean updatePackageCopiedToLocalStorage =
                copyUpdatePackageToLocalStorage(new File(updatePackagePath));

        // Validate File copied
        if (!updatePackageCopiedToLocalStorage) {
            return false;
        }

        // Start Update
        EMDKResults updateResult = startUpdateViaProfileManager();

        //
        return handleUpdateResult(updateResult);
    }

    @Override
    protected void onPostExecute(Boolean updateSuccessful) {
        super.onPostExecute(updateSuccessful);

        Log.i(TAG, "Update Complete - Success: " + updateSuccessful);

        if (!updateSuccessful) {
            if (mUpdateResult != null) {
                mCallback.onError("There was an error applying the update via Power Manager: "
                        + mUpdateResult);
            }
        } else {
            mCallback.onFinish(null);
        }
    }

    /**
     * Utility method for locating external USBs within /storage/ directory.
     * USB is located by matching regex to HEX value name of USB (E.g. ABCD-ABCD)
     *
     * @return null -> Error thrown
     * @return String -> External USB Path (Absolute)
     */
    private static String locateExternalUsb() {
        // Create File object of 'storage' Directory
        File externalUsbFolder = new File((App.DEBUGGING ? TEST_DIRECTORY : EXTERNAL_USB_DIRECTORY));

        // Validate Folder Exists
        if (!externalUsbFolder.exists()) {
            // Notify Calling Class
            mCallback.onError("Directory: " + externalUsbFolder.getAbsolutePath()
                    + " could not be located");
            // Return Null
            return null;
        }

        // Validate Folder is Directory
        if (!externalUsbFolder.isDirectory()) {
            // Notify Calling Class
            mCallback.onError("File: " + externalUsbFolder.getAbsolutePath() + " is not a directory");
            // Return Null
            return null;
        }

        // Validate USB Contains Files
        if (externalUsbFolder.listFiles().length == 0) {
            // Notify Calling Class
            mCallback.onError("No external storage devices located in: "
                    + externalUsbFolder.getAbsolutePath());
            // Return Null
            return null;
        }

        // Loop all folders -> Find USB (Hex-id matches regex)
        for (File subDirectory : externalUsbFolder.listFiles()) {
            // Match Directory Name to USB Hex-ID Regex
            if (subDirectory.getName().matches(USB_DRIVE_REGEX)) {
                // Notify Calling Class
                mCallback.onProgress("External USB Located: "
                        + subDirectory.getAbsolutePath());
                // Return USB Path
                return subDirectory.getAbsolutePath();
            }
        }

        // No USB Folder Found
        mCallback.onError("Could not locate a USB drive in: " + externalUsbFolder.getAbsolutePath());
        // Return Null
        return null;
    }

    /**
     * Utility method to trawl USB Files to locate Update Package that matches mUpdatePackageName
     *
     * @param externalUsbPath -> Path to External USB (Return from locateExternalUsb();)
     * @return null -> Update Package could not be located
     * @return String -> Update Package Path (absolute)
     */
    private static String locatedUpdatePackageInUsb(String externalUsbPath) {
        // Create File object for USB directory
        File[] usbFiles = new File(externalUsbPath).listFiles();

        // Validate Files on USB
        if (usbFiles == null || usbFiles.length == 0) {
            // Notify Calling Class
            mCallback.onError("Could not locate file: " + mUpdatePackageName + " on USB: "
                    + externalUsbPath);
            // Return Null
            return null;
        }

        // Loop USB files to locate file that matches mUpdatePackageName
        File updatePackage = null;
        for (File file : usbFiles) {
            // Check for .zip file
            if (file.getName().equalsIgnoreCase(mUpdatePackageName)) {
                updatePackage = file;
            }
        }

        // Validate Package Located
        if (updatePackage != null) {
            // Notify Calling Class
            mCallback.onProgress("Update Package: " + updatePackage.getName()
                    + " located. (" + updatePackage.getAbsolutePath() + ")");
            // Return Package Path
            return updatePackage.getAbsolutePath();
        }

        // Update Package not Located
        mCallback.onError("Could not locate Update Package: " + mUpdatePackageName);
        // Return Null
        return null;
    }

    private static boolean copyUpdatePackageToLocalStorage(File usbUpdatePackage) {
        // Create Application Directory if Required
        File internalApplicationDirectory = new File(INTERNAL_APP_DIRECTORY);
        if (!internalApplicationDirectory.exists()) {
            if (internalApplicationDirectory.mkdirs()) {
                // Notify Calling Class
                mCallback.onProgress("Created internal directory: "
                        + internalApplicationDirectory.getAbsolutePath());
            } else {
                // Notify Calling Class
                mCallback.onError("Could not create directory: "
                        + internalApplicationDirectory.getAbsolutePath());
                // Return False
                return false;
            }
        }

        // Create New File
        File localUpdatePackage = new File(INTERNAL_APP_DIRECTORY
                + File.separator + usbUpdatePackage.getName());

        // Notify Calling Class
        mCallback.onProgress("Copying File: " + usbUpdatePackage.getName()
                + " to Local storage");

        // Attempt Copy (Repeat 5 times if unsuccessful)
        for (int i = 0; i < 6; i++) {
            if (copyFile(usbUpdatePackage, localUpdatePackage)) {
                mCallback.onProgress("Successfully copied: "
                        + usbUpdatePackage.getName() + " to: "
                        + localUpdatePackage.getAbsolutePath());
                return true;
            }
        }

        // Copy Was unsuccessful
        mCallback.onError("Failed to copy package: " + mUpdatePackageName);
        // Return Null
        return false;
    }

    private static boolean copyFile(File sourceFile, File destinationFile) {
        try {
            // Copy File
            FileUtils.copyFile(sourceFile, destinationFile);

            // Check File Copy Success
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static EMDKResults startUpdateViaProfileManager() {
        // Notify Calling Class
        mCallback.onProgress("Applying "
                + (mUpdateType == UpdateType.UPGRADE ? "Update" : "Downgrade")
                + "via Power Manager. Using package: " + mUpdatePackageName);
        // Get Local Update Package Path
        File localUpdatePackage = new File(INTERNAL_APP_DIRECTORY + mUpdatePackageName);
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
                             "        <parm name=\"ZipFile\" value=\"" + localUpdatePackage.getAbsolutePath() + "\"/>\n" +
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
                             "        <parm name=\"ZipFile\" value=\"" + localUpdatePackage.getAbsolutePath() + "\"/>\n" +
                             "      </characteristic>\n" +
                             "    </characteristic>\n" +
                             "  </characteristic> ";

                break;
        }

        // Start Profile Manager
        return App.mProfileManager.processProfile("update", ProfileManager.PROFILE_FLAG.SET, profile);
    }

    private boolean handleUpdateResult(EMDKResults updateResult) {
        // Boolean Holder
        boolean updateSuccessful;

        //
        mUpdateResult = updateResult.statusCode + " | " + updateResult.extendedStatusCode + " | " + updateResult.getStatusString();

        switch (updateResult.statusCode) {
            case SUCCESS:
            case CHECK_XML:
                updateSuccessful = true;
                break;
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
                updateSuccessful = false;
                break;
            default:
                updateSuccessful = true;
                break;
        }

        return updateSuccessful;
    }
}
