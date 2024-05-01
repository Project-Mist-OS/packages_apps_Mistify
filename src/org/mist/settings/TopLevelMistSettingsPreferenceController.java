/*
 * Copyright (C) 2019-2024 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class TopLevelMistSettingsPreferenceController extends BasePreferenceController {

    public TopLevelMistSettingsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
