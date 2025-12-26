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
import android.content.res.TypedArray;
import android.provider.Settings;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class SwitchStylePreference extends Preference {

    private static final String TAG = "SwitchStylePreference";
    
    private Context mContext;
    private RecyclerView mRecyclerView;
    private SwitchStyleAdapter mAdapter;
    private int mSelectedStyle = 0;

    // Switch style data
    private static final SwitchStyleData[] SWITCH_STYLES = {
        new SwitchStyleData("Stock", R.drawable.switch_preview_stock, 0),
        new SwitchStyleData("Material Design 2", R.drawable.switch_preview_md2, 1),
        new SwitchStyleData("One UI", R.drawable.switch_preview_oneui, 2),
        new SwitchStyleData("iOS", R.drawable.switch_preview_ios, 3),
        new SwitchStyleData("Expressive", R.drawable.switch_preview_expressive, 4),
        new SwitchStyleData("Fluent", R.drawable.switch_preview_fluent, 5),
        new SwitchStyleData("Neumorphic", R.drawable.switch_preview_neumorphic, 6),
        new SwitchStyleData("Retro", R.drawable.switch_preview_retro, 7),
        new SwitchStyleData("Minimal", R.drawable.switch_preview_minimal, 8),
        new SwitchStyleData("Custom", R.drawable.switch_preview_custom, 9)
    };

    public SwitchStylePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.preference_switch_style);
        
        // Load current selection
        mSelectedStyle = Settings.System.getIntForUser(context.getContentResolver(),
                "switch_style", 0, UserHandle.USER_CURRENT);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        
        mRecyclerView = (RecyclerView) holder.findViewById(R.id.switch_style_recycler);
        if (mRecyclerView != null) {
            setupRecyclerView();
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(mContext, 2);
        mRecyclerView.setLayoutManager(layoutManager);
        
        mAdapter = new SwitchStyleAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    private class SwitchStyleAdapter extends RecyclerView.Adapter<SwitchStyleViewHolder> {

        @Override
        public SwitchStyleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext)
                    .inflate(R.layout.switch_style_item, parent, false);
            return new SwitchStyleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SwitchStyleViewHolder holder, int position) {
            SwitchStyleData data = SWITCH_STYLES[position];
            holder.bind(data, position == mSelectedStyle);
        }

        @Override
        public int getItemCount() {
            return SWITCH_STYLES.length;
        }
    }

    private class SwitchStyleViewHolder extends RecyclerView.ViewHolder {
        private ImageView mPreviewImage;
        private TextView mStyleName;
        private Switch mPreviewSwitch;
        private LinearLayout mContainer;
        private View mSelectionIndicator;

        public SwitchStyleViewHolder(View itemView) {
            super(itemView);
            mPreviewImage = itemView.findViewById(R.id.switch_preview_image);
            mStyleName = itemView.findViewById(R.id.switch_style_name);
            mPreviewSwitch = itemView.findViewById(R.id.switch_preview);
            mContainer = itemView.findViewById(R.id.switch_style_container);
            mSelectionIndicator = itemView.findViewById(R.id.selection_indicator);
        }

        public void bind(SwitchStyleData data, boolean isSelected) {
            mStyleName.setText(data.name);
            mPreviewImage.setImageResource(data.previewRes);
            
            // Set selection state
            mSelectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            mContainer.setSelected(isSelected);
            
            // Configure preview switch
            mPreviewSwitch.setChecked(true);
            mPreviewSwitch.setClickable(false);
            
            // Handle click
            mContainer.setOnClickListener(v -> {
                int oldSelection = mSelectedStyle;
                mSelectedStyle = data.styleId;
                
                // Update UI
                mAdapter.notifyItemChanged(oldSelection);
                mAdapter.notifyItemChanged(mSelectedStyle);
                
                // Notify preference change
                if (getOnPreferenceChangeListener() != null) {
                    getOnPreferenceChangeListener().onPreferenceChange(
                            SwitchStylePreference.this, String.valueOf(mSelectedStyle));
                }
            });
        }
    }

    private static class SwitchStyleData {
        final String name;
        final int previewRes;
        final int styleId;

        SwitchStyleData(String name, int previewRes, int styleId) {
            this.name = name;
            this.previewRes = previewRes;
            this.styleId = styleId;
        }
    }

    public void setSelectedStyle(int style) {
        if (mSelectedStyle != style) {
            int oldSelection = mSelectedStyle;
            mSelectedStyle = style;
            
            if (mAdapter != null) {
                mAdapter.notifyItemChanged(oldSelection);
                mAdapter.notifyItemChanged(mSelectedStyle);
            }
        }
    }

    public int getSelectedStyle() {
        return mSelectedStyle;
    }
}
