/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.preferences.colorpicker;

import android.content.Context;
import android.util.AttributeSet;

/** Lets the hosting fragment close color dialogs when its view is destroyed. */
public class MistBarColorPickerPreference extends ColorPickerPreference {
    public MistBarColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void dismissDialog() {
        if (mDialog != null) {
            mDialog.setOnColorChangedListener(null);
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
