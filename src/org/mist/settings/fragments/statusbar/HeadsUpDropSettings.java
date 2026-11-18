/*
 * Copyright (C) 2026 MistOS
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

package org.mist.settings.fragments.statusbar;

import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.mist.settings.utils.SystemUtils;

public class HeadsUpDropSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ENABLED = "mist_heads_up_drop_enabled";
    private static final String KEY_ORIGIN = "mist_heads_up_drop_origin";
    private static final String KEY_CUSTOM_X = "mist_heads_up_drop_custom_x";
    private static final String KEY_OFFSET_Y = "mist_heads_up_drop_offset_y";
    private static final String KEY_EXIT = "mist_heads_up_drop_exit";
    private static final String KEY_SPEED = "mist_heads_up_drop_speed";

    private static final int ORIGIN_CUSTOM = 6;

    private static final String[] RESTART_KEYS = {
            KEY_ENABLED, KEY_ORIGIN, KEY_CUSTOM_X, KEY_OFFSET_Y, KEY_EXIT, KEY_SPEED
    };

    private Preference mCustomX;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.heads_up_drop_settings);

        for (String key : RESTART_KEYS) {
            Preference pref = findPreference(key);
            if (pref != null) {
                pref.setOnPreferenceChangeListener(this);
            }
        }

        mCustomX = findPreference(KEY_CUSTOM_X);
        int origin = Settings.Secure.getIntForUser(getContentResolver(), KEY_ORIGIN, 0,
                UserHandle.USER_CURRENT);
        updateCustomXVisibility(origin);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_ORIGIN.equals(preference.getKey())) {
            try {
                updateCustomXVisibility(Integer.parseInt(String.valueOf(newValue)));
            } catch (NumberFormatException ignored) {
            }
        }
        SystemUtils.showSystemUiRestartDialog(getContext());
        return true;
    }

    private void updateCustomXVisibility(int origin) {
        if (mCustomX != null) {
            mCustomX.setVisible(origin == ORIGIN_CUSTOM);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MIST;
    }
}
