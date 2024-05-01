/*
 * Copyright (C) 2019-2024 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class MistSettings extends DashboardFragment {

    private static final String TAG = "MistSettings";

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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mist_settings);
}
