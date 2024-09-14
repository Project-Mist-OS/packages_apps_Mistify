/*
 * Copyright (C) 2023 The MistOS Project
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

package com.mist.mistify.categories;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import com.android.internal.util.mist.udfps.CustomUdfpsUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.mist.support.preferences.GlobalSettingListPreference;

import com.android.internal.util.mist.MistUtils;
import com.mist.support.preferences.SystemSettingListPreference;
import com.android.settings.utils.SystemUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class ThemeSettings extends SettingsPreferenceFragment 
            implements Preference.OnPreferenceChangeListener {

    private static final String UDFPS_CATEGORY = "udfps_category";

    private static final String SETTINGS_DASHBOARD_STYLE = "settings_dashboard_style";

    private static final String KEY_LOCK_SOUND = "lock_sound";
    private static final String KEY_UNLOCK_SOUND = "unlock_sound";


    private SystemSettingListPreference mSettingsDashBoardStyle;
    private PreferenceCategory mUdfpsCategory;

    private GlobalSettingListPreference mLockSound;
    private GlobalSettingListPreference mUnlockSound;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.mist_theme);
        PreferenceScreen prefSet = getPreferenceScreen();
        final Resources res = getResources();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mSettingsDashBoardStyle = (SystemSettingListPreference) findPreference(SETTINGS_DASHBOARD_STYLE);
        mSettingsDashBoardStyle.setOnPreferenceChangeListener(this);

        mLockSound = (GlobalSettingListPreference) findPreference(KEY_LOCK_SOUND);
        mLockSound.setOnPreferenceChangeListener(this);
        mUnlockSound = (GlobalSettingListPreference) findPreference(KEY_UNLOCK_SOUND);
        mUnlockSound.setOnPreferenceChangeListener(this);

        mUdfpsCategory = findPreference(UDFPS_CATEGORY);
        if (!CustomUdfpsUtils.hasUdfpsSupport(getContext())) {
            prefScreen.removePreference(mUdfpsCategory);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        ContentResolver resolver = getActivity().getContentResolver();
	if (preference == mSettingsDashBoardStyle){
            MistUtils.showSystemUiRestartDialog(getContext());
            return true;
            } else if (preference == mLockSound || preference == mUnlockSound) {
            MistUtils.showSystemUiRestartDialog(getContext());
            return true;
           }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MIST;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.mist_theme;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
