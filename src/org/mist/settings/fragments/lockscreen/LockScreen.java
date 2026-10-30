/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.fragments.lockscreen;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.android.OmniJawsClient;
import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

import org.mist.settings.preferences.SecureSettingSwitchPreference;
import org.mist.settings.utils.DeviceUtils;
import org.mist.settings.utils.PreferenceUtils;
import org.mist.settings.utils.SystemUtils;

@SearchIndexable
public class LockScreen extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockScreen";

    private static final String KEY_FP_ERROR = "fp_error_vibrate";
    private static final String KEY_FP_SUCCESS = "fp_success_vibrate";
    private static final String KEY_RIPPLE_EFFECT = "enable_ripple_effect";
    private static final String KEY_SMARTSPACE = "lockscreen_smartspace_enabled";
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";
    private static final String LOCKSCREEN_GESTURES_CATEGORY = "lockscreen_gestures_category";

    private Preference mRippleEffect;
    private SwitchPreferenceCompat mFpErrorVib;
    private SwitchPreferenceCompat mFpSuccessVib;
    private SwitchPreferenceCompat mSmartspace;
    private SwitchPreferenceCompat mWeather;
    private Preference mStackedNotifications;
    private final ContentObserver mCompactNotificationsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    updateStackedNotificationsPreference();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mist_settings_lock_screen);

        final Context context = getContext();

        final PreferenceCategory gestCategory = findPreference(LOCKSCREEN_GESTURES_CATEGORY);

        mFpSuccessVib = findPreference(KEY_FP_SUCCESS);
        mFpErrorVib = findPreference(KEY_FP_ERROR);
        mRippleEffect = findPreference(KEY_RIPPLE_EFFECT);

        boolean hasFingerprint = DeviceUtils.hasFingerprint(context);
        if (!hasFingerprint) {
            gestCategory.removePreference(mRippleEffect);
        }

        if (!hasFingerprint || !DeviceUtils.hasVibrator(context)) {
            gestCategory.removePreference(mFpSuccessVib);
            gestCategory.removePreference(mFpErrorVib);
        }

        mSmartspace = findPreference(KEY_SMARTSPACE);
        mSmartspace.setOnPreferenceChangeListener(this);

        mWeather = findPreference(KEY_WEATHER);
        mWeather.setOnPreferenceChangeListener(this);

        mStackedNotifications = findPreference(
                Settings.Secure.LOCK_SCREEN_STACKED_NOTIFICATIONS);
        updateStackedNotificationsPreference();

        updateWeatherSettings();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSmartspace) {
            updateWeatherSettings();
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mWeather) {
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        }
        return false;
    }

    private void updateWeatherSettings() {
        boolean weatherEnabled = OmniJawsClient.get().isOmniJawsEnabled(getContext());
        mWeather.setEnabled(!mSmartspace.isChecked() && weatherEnabled);
        mWeather.setSummary(!mSmartspace.isChecked() && weatherEnabled
                ? R.string.lockscreen_weather_summary
                : R.string.lockscreen_weather_enabled_info);
    }

    private void updateStackedNotificationsPreference() {
        if (mStackedNotifications == null) {
            return;
        }
        boolean compactEnabled = Flags.notificationMinimalism()
                && Settings.Secure.getIntForUser(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_NOTIFICATION_MINIMALISM, 1,
                        UserHandle.USER_CURRENT) == 1;
        mStackedNotifications.setEnabled(!compactEnabled);
        mStackedNotifications.setSummary(compactEnabled
                ? R.string.lock_screen_stacked_notifications_compact_summary
                : R.string.lock_screen_stacked_notifications_summary);
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_NOTIFICATION_MINIMALISM),
                false, mCompactNotificationsObserver, UserHandle.USER_CURRENT);
        updateStackedNotificationsPreference();
        updateWeatherSettings();
        PreferenceUtils.reloadCustomPrimarySwitches(getPreferenceScreen());
    }

    @Override
    public void onPause() {
        getContentResolver().unregisterContentObserver(mCompactNotificationsObserver);
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MIST;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mist_settings_lock_screen) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    boolean hasFingerprint = DeviceUtils.hasFingerprint(context);
                    if (!hasFingerprint) {
                        keys.add(KEY_RIPPLE_EFFECT);
                    }

                    if (!hasFingerprint || !DeviceUtils.hasVibrator(context)) {
                        keys.add(KEY_FP_SUCCESS);
                        keys.add(KEY_FP_ERROR);
                    }

                    return keys;
                }
            };
}
