package com.zebra.jamesswinton.localupdater.AsyncUpdaters;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.zebra.jamesswinton.localupdater.App;
import com.zebra.jamesswinton.localupdater.Interfaces.UpdateProgressInterface;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class ManualAsyncUpdater extends AsyncTask<Void, String, Void> {

    // Debugging
    private static final String TAG = "ManualAsyncUpdater";

    // Constants
    private static final String USB_DRIVE_REGEX = "(([A-Z0-9]{4})(-)([A-Z0-9]{4}))";
    private static final String TEST_DIRECTORY = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "Test Directory";
    private static final String EXTERNAL_USB_DIRECTORY = File.separator + "storage" + File.separator;
    private static final String INTERNAL_APP_DIRECTORY = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + File.separator + "USB Updater" + File.separator;

    // Non-static Variables


    // Static Variables
    private static List<File> mLocalUpdatePackages;
    private static UpdateProgressInterface mCallback;

    public ManualAsyncUpdater(UpdateProgressInterface callback) {
        //
        mLocalUpdatePackages = new ArrayList<>();
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
    protected Void doInBackground(Void... voids) {

        // Locate USB
        String externalUsbPath = locateExternalUsb();

        // Validate USB Path (Or stop)
        if (externalUsbPath == null) {
            return null;
        }

        // Locate Update Package
        List<File> updatePackages = locateAllUpdatePackagesInUsb(externalUsbPath);

        // Validate Update Package (Or stop)
        if (updatePackages == null) {
            return null;
        }

        // Copy File to Local Storage
        boolean updatePackageCopiedToLocalStorage =
                copyAllUpdatePackagesToLocalStorage(updatePackages);

        // Validate File copied
        if (!updatePackageCopiedToLocalStorage) {
            return null;
        }

        // Finish -> Return LocalUpdatePackages & Exit
        mLocalUpdatePackages = new ArrayList<>(Arrays.asList(new File(INTERNAL_APP_DIRECTORY).listFiles()));
        mCallback.onFinish(mLocalUpdatePackages);
        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        super.onPostExecute(param);

        Log.i(TAG, "Update Complete - Success: ");
    }

    /**
     * Utility method for locating external USBs within /storage/ directory.
     * USB is located by matching regex to HEX value name of USB (E.g. ABCD-ABCD)
     *
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
     * @return String -> Update Package Path (absolute)
     */
    private static List<File> locateAllUpdatePackagesInUsb(String externalUsbPath) {
        // Create File object for USB directory
        File[] usbFiles = new File(externalUsbPath).listFiles();

        // Validate Files on USB
        if (usbFiles == null || usbFiles.length == 0) {
            // Notify Calling Class
            mCallback.onError("Could not locate any files on USB: " + externalUsbPath);
            // Return Null
            return null;
        }

        // Loop USB files to locate file that matches mUpdatePackageName
        List<File> updatePackages = new ArrayList<>();
        for (File file : usbFiles) {
            // Check for .zip file
            if (file.getName().endsWith(".zip")) {
                // Store file
                updatePackages.add(file);
            }
        }

        // Validate Package Located
        if (!updatePackages.isEmpty()) {
            // Notify Calling Class
            mCallback.onProgress("Located " + updatePackages.size() + " Update Packages");
            // Return Package Path
            return updatePackages;
        }

        // Update Package not Located
        mCallback.onError("Could not locate any Update Packages on USB: " + externalUsbPath);
        // Return Null
        return null;
    }

    private static boolean copyAllUpdatePackagesToLocalStorage(List<File> usbUpdatePackages) {
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

        // Loop through all packages
        boolean copySuccess = false;
        for (File updatePackage : usbUpdatePackages) {
            // Notify Calling Class
            mCallback.onProgress("Copying File: " + updatePackage.getName()
                    + " to Local storage");

            // Create New File
            File localUpdatePackage = new File(INTERNAL_APP_DIRECTORY
                    + File.separator + updatePackage.getName());

            // Attempt Copy (Repeat 5 times if unsuccessful)
            for (int i = 0; i < 6; i++) {
                if (copyFile(updatePackage, localUpdatePackage)) {
                    // Update Holder Variable
                    copySuccess = true;
                    // Add File to List
                    mLocalUpdatePackages.add(localUpdatePackage);
                    // Notify Calling Class
                    mCallback.onProgress("Successfully copied: "
                            + updatePackage.getName() + " to: "
                            + localUpdatePackage.getAbsolutePath());
                    break;
                } else {
                    // Update Holder Variable
                    copySuccess = false;
                    // Notify Calling Class
                    mCallback.onProgress("Failed to copy: "
                            + updatePackage.getName() + " to: "
                            + localUpdatePackage.getAbsolutePath());
                }
            }
        }

        // Notify Calling Class
        if (copySuccess) {
            mCallback.onProgress("Successfully copied all packages");
        } else {
            mCallback.onError("Failed to copy packages");
        }
        // Return Success / Failure
        return copySuccess;
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


}