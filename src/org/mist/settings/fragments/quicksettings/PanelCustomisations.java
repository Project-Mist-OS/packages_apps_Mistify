/*
 * SPDX-FileCopyrightText: MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.fragments.quicksettings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import lineageos.providers.LineageSettings;

import org.mist.settings.preferences.SecureSettingListPreference;
import org.mist.settings.utils.DeviceUtils;
import org.mist.settings.utils.SystemUtils;

@SearchIndexable
public class PanelCustomisations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String TAG = "PanelCustomisations";

    private static final String QS_BRIGHTNESS_CATEGORY = "ax_qs_sliders_category";

    private static final String KEY_AX_QS_PANEL_MODE = "ax_qs_panel_mode";
    private static final String KEY_AX_QS_QUICK_PANEL_ON_LEFT = "ax_qs_quick_panel_on_left";
    private static final String KEY_AX_QS_BRIGHTNESS_SLIDER_STYLE = "ax_qs_brightness_vertical_slider_style";
    private static final String KEY_AX_QS_VOLUME_SLIDER_STYLE = "ax_qs_volume_vertical_slider_style";
    private static final String KEY_QS_RESET_LAYOUT = "qs_reset_layout";

    private static final String KEY_IMAGE_FILL_PICK = "ax_qs_image_fill_pick";
    private static final String KEY_IMAGE_FILL_CLEAR = "ax_qs_image_fill_clear";
    private static final String SETTING_IMAGE_FILL_URI = "ax_qs_image_fill_uri";
    private static final int REQUEST_PICK_IMAGE_FILL = 20001;

    // The image is copied into MediaStore so it outlives the picker's temporary grant, a reboot
    // and a SystemUI restart. Only the uri is kept in secure settings.
    private static final String IMAGE_FILL_RELATIVE_PATH = "Pictures/QSTileImage";
    private static final String IMAGE_FILL_DISPLAY_NAME = "qs_tile_image";

    private static final String KEY_BRIGHTNESS_SLIDER_HAPTIC = "qs_brightness_slider_haptic";
    private static final String KEY_BRIGHTNESS_SLIDER_POSITION = "qs_brightness_slider_position";
    private static final String KEY_SHOW_AUTO_BRIGHTNESS = "qs_show_auto_brightness";
    private static final String KEY_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";

    private SecureSettingListPreference mQsPanelMode;
    private SecureSettingListPreference mQsQuickPanelOnLeft;
    private SecureSettingListPreference mQsBrightnessSliderStyle;
    private SecureSettingListPreference mQsVolumeSliderStyle;
    private Preference mQsResetLayout;
    private Preference mImageFillPick;
    private Preference mImageFillClear;

    private ListPreference mBrightnessSliderPosition;
    private ListPreference mShowBrightnessSlider;
    private SwitchPreferenceCompat mBrightnessSliderHaptic;
    private SwitchPreferenceCompat mShowAutoBrightness;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mist_settings_panel_customisations);

        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();

        final PreferenceCategory brightnessCategory = findPreference(QS_BRIGHTNESS_CATEGORY);

        mQsPanelMode = findPreference(KEY_AX_QS_PANEL_MODE);
        if (mQsPanelMode != null) {
            mQsPanelMode.setOnPreferenceChangeListener(this);
        }

        mQsQuickPanelOnLeft = findPreference(KEY_AX_QS_QUICK_PANEL_ON_LEFT);
        updateQuickPanelOnLeftState();

        mQsBrightnessSliderStyle = findPreference(KEY_AX_QS_BRIGHTNESS_SLIDER_STYLE);
        if (mQsBrightnessSliderStyle != null) {
            mQsBrightnessSliderStyle.setOnPreferenceChangeListener(this);
        }

        mQsVolumeSliderStyle = findPreference(KEY_AX_QS_VOLUME_SLIDER_STYLE);
        if (mQsVolumeSliderStyle != null) {
            mQsVolumeSliderStyle.setOnPreferenceChangeListener(this);
        }

        mQsResetLayout = findPreference(KEY_QS_RESET_LAYOUT);
        if (mQsResetLayout != null) {
            mQsResetLayout.setOnPreferenceClickListener(this);
        }

        mImageFillPick = findPreference(KEY_IMAGE_FILL_PICK);
        if (mImageFillPick != null) {
            mImageFillPick.setOnPreferenceClickListener(this);
        }
        mImageFillClear = findPreference(KEY_IMAGE_FILL_CLEAR);
        if (mImageFillClear != null) {
            mImageFillClear.setOnPreferenceClickListener(this);
        }

        mShowBrightnessSlider = findPreference(KEY_SHOW_BRIGHTNESS_SLIDER);
        if (mShowBrightnessSlider != null) {
            mShowBrightnessSlider.setOnPreferenceChangeListener(this);
            boolean showSlider = LineageSettings.Secure.getIntForUser(resolver,
                    LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT) > 0;

            mBrightnessSliderPosition = findPreference(KEY_BRIGHTNESS_SLIDER_POSITION);
            if (mBrightnessSliderPosition != null) {
                mBrightnessSliderPosition.setEnabled(showSlider);
            }

            mBrightnessSliderHaptic = findPreference(KEY_BRIGHTNESS_SLIDER_HAPTIC);
            if (mBrightnessSliderHaptic != null) {
                if (DeviceUtils.hasVibrator(context)) {
                    mBrightnessSliderHaptic.setOnPreferenceChangeListener(this);
                    mBrightnessSliderHaptic.setEnabled(showSlider);
                } else if (brightnessCategory != null) {
                    brightnessCategory.removePreference(mBrightnessSliderHaptic);
                }
            }

            mShowAutoBrightness = findPreference(KEY_SHOW_AUTO_BRIGHTNESS);
            if (mShowAutoBrightness != null) {
                if (context.getResources().getBoolean(
                        com.android.internal.R.bool.config_automatic_brightness_available)) {
                    mShowAutoBrightness.setEnabled(showSlider);
                } else if (brightnessCategory != null) {
                    brightnessCategory.removePreference(mShowAutoBrightness);
                }
            }
        }
    }

    private void updateQuickPanelOnLeftState() {
        if (mQsQuickPanelOnLeft != null) {
            int panelMode = Settings.Secure.getIntForUser(
                    getContext().getContentResolver(),
                    "ax_qs_panel_mode", 0, UserHandle.USER_CURRENT);
            mQsQuickPanelOnLeft.setEnabled(panelMode == 1);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mShowBrightnessSlider) {
            int value = Integer.parseInt((String) newValue);
            if (mBrightnessSliderPosition != null)
                mBrightnessSliderPosition.setEnabled(value > 0);
            if (mBrightnessSliderHaptic != null)
                mBrightnessSliderHaptic.setEnabled(value > 0);
            if (mShowAutoBrightness != null)
                mShowAutoBrightness.setEnabled(value > 0);
            return true;
        } else if (preference == mQsPanelMode) {
            int mode = Integer.parseInt((String) newValue);
            if (mQsQuickPanelOnLeft != null) {
                mQsQuickPanelOnLeft.setEnabled(mode == 1);
            }
            return true;
        } else if (preference == mQsBrightnessSliderStyle) {
            int val = Integer.parseInt((String) newValue);
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    "ax_qs_brightness_vertical_slider_style", val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mQsVolumeSliderStyle) {
            int val = Integer.parseInt((String) newValue);
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    "ax_qs_volume_vertical_slider_style", val, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mBrightnessSliderHaptic) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mQsResetLayout) {
            final ContentResolver resolver = getContext().getContentResolver();
            final int userId = UserHandle.USER_CURRENT;
            Settings.Secure.putStringForUser(resolver, "ax_qqs_tile_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_tile_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qqs_control_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_control_order", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qqs_spans", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_spans", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qqs_control_positions", "", userId);
            Settings.Secure.putStringForUser(resolver, "ax_qs_control_positions", "", userId);
            Toast.makeText(getContext(), R.string.ax_qs_reset_layout_done, Toast.LENGTH_SHORT).show();
            return true;
        } else if (preference == mImageFillPick) {
            pickImageFill();
            return true;
        } else if (preference == mImageFillClear) {
            clearImageFill();
            Toast.makeText(getContext(), R.string.ax_qs_image_fill_cleared, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void pickImageFill() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"image/jpeg", "image/png", "image/webp"});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_PICK_IMAGE_FILL);
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.ax_qs_image_fill_needs_gallery,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_IMAGE_FILL || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }

        // Let the user choose which part of the image to show before anything is saved.
        QsImageCropDialog.show(getActivity(), data.getData(), new QsImageCropDialog.Callback() {
            @Override
            public void onCropped(Bitmap bitmap) {
                saveCroppedImageFill(bitmap);
            }

            @Override
            public void onFailed() {
                if (isAdded()) {
                    Toast.makeText(getContext(), R.string.ax_qs_image_fill_failed,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveCroppedImageFill(Bitmap bitmap) {
        final Context context = getContext().getApplicationContext();
        final Activity activity = getActivity();
        new Thread(() -> {
            final ContentResolver cr = context.getContentResolver();
            final Uri saved = saveImageFill(cr, bitmap);
            if (saved != null) {
                // Only switch to the new image once it was fully written, then drop old copies.
                Settings.Secure.putStringForUser(cr, SETTING_IMAGE_FILL_URI, saved.toString(),
                        UserHandle.USER_CURRENT);
                deleteImageFillCopies(cr, ContentUris.parseId(saved));
            }
            bitmap.recycle();
            if (activity != null) {
                activity.runOnUiThread(() -> Toast.makeText(context,
                        saved != null ? R.string.ax_qs_image_fill_set
                                : R.string.ax_qs_image_fill_failed,
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /** Writes the crop losslessly, so no detail is lost compared to the original pixels. */
    private Uri saveImageFill(ContentResolver cr, Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,
                IMAGE_FILL_DISPLAY_NAME + "_" + System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/webp");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, IMAGE_FILL_RELATIVE_PATH);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri out = null;
        try {
            out = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (out == null) return null;

            try (OutputStream os = cr.openOutputStream(out, "wt")) {
                if (os == null || !bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, os)) {
                    cr.delete(out, null, null);
                    return null;
                }
                os.flush();
            }

            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            cr.update(out, done, null, null);
            return out;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save QS image", e);
            if (out != null) {
                try {
                    cr.delete(out, null, null);
                } catch (Exception ignored) {
                }
            }
            return null;
        }
    }

    private void clearImageFill() {
        final ContentResolver cr = getContext().getContentResolver();
        Settings.Secure.putStringForUser(cr, SETTING_IMAGE_FILL_URI, "", UserHandle.USER_CURRENT);
        deleteImageFillCopies(cr, -1);
    }

    private void deleteImageFillCopies(ContentResolver cr, long keepId) {
        try {
            cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.MediaColumns.RELATIVE_PATH + "=? AND "
                            + MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ? AND "
                            + MediaStore.MediaColumns._ID + "!=?",
                    new String[]{IMAGE_FILL_RELATIVE_PATH + "/", IMAGE_FILL_DISPLAY_NAME + "%",
                            String.valueOf(keepId)});
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete old QS images", e);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MIST;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider(
            R.xml.mist_settings_panel_customisations) {

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                boolean enabled) {
            // These settings only apply to the Mistify custom QS style.
            if (Settings.Secure.getIntForUser(context.getContentResolver(),
                    "ax_qs_style", 0, UserHandle.USER_CURRENT) != 1) {
                return new ArrayList<>();
            }
            return super.getXmlResourcesToIndex(context, enabled);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> keys = super.getNonIndexableKeys(context);

            if (!context.getResources().getBoolean(
                    com.android.internal.R.bool.config_automatic_brightness_available)) {
                keys.add(KEY_SHOW_AUTO_BRIGHTNESS);
            }

            if (!DeviceUtils.hasVibrator(context)) {
                keys.add(KEY_BRIGHTNESS_SLIDER_HAPTIC);
            }

            return keys;
        }
    };
}
