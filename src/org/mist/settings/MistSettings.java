/*
 * Copyright (C) 2019-2024 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceFragmentCompat;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.instrumentation.Instrumentable;

@SearchIndexable
public class MistSettings extends DashboardFragment implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TAG = "MistSettings";
    private static final String KEY_HOME_STYLE = "mist_settings_home_style";
    private static final int STYLE_FREEFLOW = 1;
    private static final String FREEFLOW_MISTIFY = "com.mist.freeflow.FreeFlowMistifyActivity";
    private static final String HOMEPAGE = "com.android.settings.homepage.SettingsHomepageActivity";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        if (activity == null || HOMEPAGE.equals(activity.getClass().getName())
                || Settings.System.getInt(activity.getContentResolver(), KEY_HOME_STYLE, 0)
                        != STYLE_FREEFLOW) {
            return;
        }
        final Intent intent = new Intent().setClassName(activity, FREEFLOW_MISTIFY);
        if (activity.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY) != null) {
            startActivity(intent);
            activity.finish();
            activity.overridePendingTransition(0, 0);
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.mist_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.MIST;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        new SubSettingLauncher(getActivity())
                .setDestination(pref.getFragment())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(caller instanceof Instrumentable
                        ? ((Instrumentable) caller).getMetricsCategory()
                        : Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .setTitleRes(-1)
                .launch();
        return true;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        final PreferenceScreen screen = getPreferenceScreen();
        // Tint the homepage icons
        final int tintColor = Utils.getHomepageIconColor(getContext());
        final int count = screen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            final Preference preference = screen.getPreference(i);
            if (preference == null) {
                break;
            }
            final Drawable icon = preference.getIcon();
            if (icon != null) {
                icon.setTint(tintColor);
            }
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mist_settings);
}