package com.zebra.jamesswinton.localupdater.Interfaces;

import androidx.annotation.Nullable;
import java.io.File;
import java.util.List;

public interface UpdateProgressInterface {
    void onStart();
    void onProgress(String progressText);
    void onFinish(@Nullable List<File> updatePackages);
    void onError(String error);
}
