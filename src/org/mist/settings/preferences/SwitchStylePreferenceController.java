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

package org.mist.settings.preferences;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;

public class SwitchStylePreferenceController extends ListPreference implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "SwitchStylePreferenceController";
    
    private static final String SWITCH_STYLE_KEY = "switch_style";
    
    // Switch style overlay packages
    private static final String[] SWITCH_STYLE_OVERLAYS = {
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

    private IOverlayManager mOverlayManager;
    private Context mContext;

    public SwitchStylePreferenceController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        
        setKey(SWITCH_STYLE_KEY);
        setTitle(R.string.switch_style_title);
        setSummary(R.string.switch_style_summary);
        setDialogTitle(R.string.switch_style_dialog_title);
        
        setupEntries();
        setOnPreferenceChangeListener(this);
        updateSummary();
    }

    private void setupEntries() {
        String[] entries = {
            mContext.getString(R.string.switch_style_stock),
            mContext.getString(R.string.switch_style_md2),
            mContext.getString(R.string.switch_style_oneui),
            mContext.getString(R.string.switch_style_ios),
            mContext.getString(R.string.switch_style_expressive),
            mContext.getString(R.string.switch_style_fluent),
            mContext.getString(R.string.switch_style_neumorphic),
            mContext.getString(R.string.switch_style_retro),
            mContext.getString(R.string.switch_style_minimal),
            mContext.getString(R.string.switch_style_custom)
        };
        
        String[] entryValues = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
        };
        
        setEntries(entries);
        setEntryValues(entryValues);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        int switchStyle = Integer.parseInt(value);
        
        // Disable all switch overlays first
        for (String overlay : SWITCH_STYLE_OVERLAYS) {
            try {
                mOverlayManager.setEnabled(overlay, false, UserHandle.USER_CURRENT);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to disable overlay: " + overlay, e);
            }
        }
        
        // Enable the selected overlay (skip stock which is index 0)
        if (switchStyle > 0 && switchStyle < SWITCH_STYLE_OVERLAYS.length) {
            try {
                mOverlayManager.setEnabled(SWITCH_STYLE_OVERLAYS[switchStyle], 
                        true, UserHandle.USER_CURRENT);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to enable overlay: " + 
                        SWITCH_STYLE_OVERLAYS[switchStyle], e);
            }
        }
        
        // Save preference
        Settings.System.putIntForUser(mContext.getContentResolver(),
                "switch_style", switchStyle, UserHandle.USER_CURRENT);
        
        setValue(value);
        updateSummary();
        return true;
    }

    private void updateSummary() {
        int currentStyle = Settings.System.getIntForUser(mContext.getContentResolver(),
                "switch_style", 0, UserHandle.USER_CURRENT);
        
        String[] entries = getEntries() != null ? 
                getEntries().clone() : new String[0];
        
        if (currentStyle >= 0 && currentStyle < entries.length) {
            setSummary(entries[currentStyle]);
            setValue(String.valueOf(currentStyle));
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        updateSummary();
    }
}
