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

package org.mist.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import org.mist.settings.preferences.SwitchStylePreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class SwitchStyles extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "SwitchStyles";
    
    private static final String PREF_SWITCH_STYLE = "switch_style_selection";
    private static final String PREF_CUSTOM_SWITCH_ENABLED = "custom_switch_enabled";
    private static final String PREF_SWITCH_ANIMATION = "switch_animation_enabled";
    private static final String PREF_SWITCH_HAPTIC = "switch_haptic_feedback";

    private SwitchStylePreference mSwitchStylePreference;
    private IOverlayManager mOverlayManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.switch_styles);
        
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        
        initializePreferences();
    }

    private void initializePreferences() {
        mSwitchStylePreference = findPreference(PREF_SWITCH_STYLE);
        if (mSwitchStylePreference != null) {
            mSwitchStylePreference.setOnPreferenceChangeListener(this);
        }
        
        // Initialize other preferences
        Preference customSwitchPref = findPreference(PREF_CUSTOM_SWITCH_ENABLED);
        if (customSwitchPref != null) {
            customSwitchPref.setOnPreferenceChangeListener(this);
        }
        
        Preference animationPref = findPreference(PREF_SWITCH_ANIMATION);
        if (animationPref != null) {
            animationPref.setOnPreferenceChangeListener(this);
        }
        
        Preference hapticPref = findPreference(PREF_SWITCH_HAPTIC);
        if (hapticPref != null) {
            hapticPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        
        switch (key) {
            case PREF_SWITCH_STYLE:
                handleSwitchStyleChange((String) newValue);
                return true;
                
            case PREF_CUSTOM_SWITCH_ENABLED:
                Settings.System.putIntForUser(getContentResolver(),
                        "custom_switch_enabled", (Boolean) newValue ? 1 : 0,
                        UserHandle.USER_CURRENT);
                return true;
                
            case PREF_SWITCH_ANIMATION:
                Settings.System.putIntForUser(getContentResolver(),
                        "switch_animation_enabled", (Boolean) newValue ? 1 : 0,
                        UserHandle.USER_CURRENT);
                return true;
                
            case PREF_SWITCH_HAPTIC:
                Settings.System.putIntForUser(getContentResolver(),
                        "switch_haptic_feedback", (Boolean) newValue ? 1 : 0,
                        UserHandle.USER_CURRENT);
                return true;
        }
        
        return false;
    }

    private void handleSwitchStyleChange(String value) {
        int switchStyle = Integer.parseInt(value);
        
        // Apply the switch style through overlay manager
        applySwitchStyle(switchStyle);
        
        // Save to settings
        Settings.System.putIntForUser(getContentResolver(),
                "switch_style", switchStyle, UserHandle.USER_CURRENT);
    }

    private void applySwitchStyle(int style) {
        String[] overlays = {
            "com.android.theme.switch.stock",
            "com.android.theme.switch.md2", 
            "com.android.theme.switch.oneui",
            "com.android.theme.switch.ios",
            "com.android.theme.switch.expressive",
            "com.android.theme.switch.fluent",
            "com.android.theme.switch.neumorphic",
            "com.android.theme.switch.retro",
            "com.android.theme.switch.minimal",
            "com.android.theme.switch.custom"
        };
        
        // Disable all overlays first
        for (String overlay : overlays) {
            try {
                mOverlayManager.setEnabled(overlay, false, UserHandle.USER_CURRENT);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to disable overlay: " + overlay, e);
            }
        }
        
        // Enable selected overlay (skip stock which is index 0)
        if (style > 0 && style < overlays.length) {
            try {
                mOverlayManager.setEnabled(overlays[style], true, UserHandle.USER_CURRENT);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to enable overlay: " + overlays[style], e);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MIST;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.switch_styles;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.switch_styles);
}
