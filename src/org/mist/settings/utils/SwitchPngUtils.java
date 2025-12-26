/*
 * Copyright (C) 2024 MistOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mist.settings.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.os.UserHandle;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SwitchPngUtils {

    private static final String TAG = "SwitchPngUtils";
    
    // PNG file paths
    private static final String SWITCH_PNG_DIR = "/data/system/theme/switch_png/";
    private static final String THUMB_ON_PNG = "switch_thumb_on.png";
    private static final String THUMB_OFF_PNG = "switch_thumb_off.png";
    private static final String TRACK_ON_PNG = "switch_track_on.png";
    private static final String TRACK_OFF_PNG = "switch_track_off.png";
    
    // Settings keys
    private static final String SWITCH_PNG_ENABLED = "switch_png_enabled";
    private static final String SWITCH_THUMB_ON_PATH = "switch_thumb_on_path";
    private static final String SWITCH_THUMB_OFF_PATH = "switch_thumb_off_path";
    private static final String SWITCH_TRACK_ON_PATH = "switch_track_on_path";
    private static final String SWITCH_TRACK_OFF_PATH = "switch_track_off_path";

    public static boolean isPngSwitchEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                SWITCH_PNG_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
    }

    public static void setPngSwitchEnabled(Context context, boolean enabled) {
        Settings.System.putIntForUser(context.getContentResolver(),
                SWITCH_PNG_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    public static void selectPngImage(Context context, String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/png");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        Intent chooser = Intent.createChooser(intent, "Select PNG Image for " + type);
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).startActivityForResult(chooser, requestCode);
        }
    }

    public static boolean savePngImage(Context context, Uri imageUri, String type) {
        try {
            // Create directory if it doesn't exist
            File dir = new File(SWITCH_PNG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Determine filename based on type
            String filename;
            String settingsKey;
            
            switch (type) {
                case "thumb_on":
                    filename = THUMB_ON_PNG;
                    settingsKey = SWITCH_THUMB_ON_PATH;
                    break;
                case "thumb_off":
                    filename = THUMB_OFF_PNG;
                    settingsKey = SWITCH_THUMB_OFF_PATH;
                    break;
                case "track_on":
                    filename = TRACK_ON_PNG;
                    settingsKey = SWITCH_TRACK_ON_PATH;
                    break;
                case "track_off":
                    filename = TRACK_OFF_PNG;
                    settingsKey = SWITCH_TRACK_OFF_PATH;
                    break;
                default:
                    return false;
            }

            File outputFile = new File(dir, filename);
            
            // Copy image to internal storage
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            inputStream.close();
            outputStream.close();
            
            // Save path to settings
            Settings.System.putStringForUser(context.getContentResolver(),
                    settingsKey, outputFile.getAbsolutePath(), UserHandle.USER_CURRENT);
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to save PNG image", e);
            return false;
        }
    }

    public static Drawable getPngDrawable(Context context, String type) {
        if (!isPngSwitchEnabled(context)) {
            return null;
        }

        String settingsKey;
        switch (type) {
            case "thumb_on":
                settingsKey = SWITCH_THUMB_ON_PATH;
                break;
            case "thumb_off":
                settingsKey = SWITCH_THUMB_OFF_PATH;
                break;
            case "track_on":
                settingsKey = SWITCH_TRACK_ON_PATH;
                break;
            case "track_off":
                settingsKey = SWITCH_TRACK_OFF_PATH;
                break;
            default:
                return null;
        }

        String imagePath = Settings.System.getStringForUser(context.getContentResolver(),
                settingsKey, UserHandle.USER_CURRENT);
        
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    return new BitmapDrawable(context.getResources(), bitmap);
                }
            }
        }
        
        return null;
    }

    public static void clearPngImages(Context context) {
        // Clear all PNG settings
        Settings.System.putStringForUser(context.getContentResolver(),
                SWITCH_THUMB_ON_PATH, "", UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(context.getContentResolver(),
                SWITCH_THUMB_OFF_PATH, "", UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(context.getContentResolver(),
                SWITCH_TRACK_ON_PATH, "", UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(context.getContentResolver(),
                SWITCH_TRACK_OFF_PATH, "", UserHandle.USER_CURRENT);
        
        // Delete PNG files
        File dir = new File(SWITCH_PNG_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    public static boolean hasPngImage(Context context, String type) {
        String settingsKey;
        switch (type) {
            case "thumb_on":
                settingsKey = SWITCH_THUMB_ON_PATH;
                break;
            case "thumb_off":
                settingsKey = SWITCH_THUMB_OFF_PATH;
                break;
            case "track_on":
                settingsKey = SWITCH_TRACK_ON_PATH;
                break;
            case "track_off":
                settingsKey = SWITCH_TRACK_OFF_PATH;
                break;
            default:
                return false;
        }

        String imagePath = Settings.System.getStringForUser(context.getContentResolver(),
                settingsKey, UserHandle.USER_CURRENT);
        
        return imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists();
    }
}
