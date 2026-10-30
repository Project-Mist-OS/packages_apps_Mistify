/*
 * SPDX-FileCopyrightText: MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.fragments.quicksettings;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full screen crop step shown after the user picks an image for the QS tiles.
 *
 * The frame is drawn on a screen sized preview, but the crop itself is decoded from the original
 * file at full resolution, so choosing a small area of a large photo does not lose detail.
 */
public final class QsImageCropDialog {

    private static final String TAG = "QsImageCropDialog";

    // Only used for the on screen preview, never for the saved crop.
    private static final int PREVIEW_MAX_SIZE_PX = 2048;

    private static final float ASPECT_SQUARE = 1f;
    private static final float ASPECT_WIDE = 2f;
    private static final float ASPECT_TALL = 0.5f;

    /** Aspect ratio value for {@link #show(Activity, Uri, float, Callback)} that locks a square. */
    public static final float INITIAL_ASPECT_SQUARE = ASPECT_SQUARE;

    public interface Callback {
        /** Called on the main thread with the full resolution crop. */
        void onCropped(Bitmap bitmap);

        /** Called on the main thread if the image could not be read. */
        void onFailed();
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private QsImageCropDialog() {}

    public static void show(Activity activity, Uri source, Callback callback) {
        show(activity, source, QsImageCropView.ASPECT_FREE, callback);
    }

    /** Like {@link #show(Activity, Uri, Callback)}, but starts with the crop locked to an aspect. */
    public static void show(Activity activity, Uri source, float initialAspect,
            Callback callback) {
        EXECUTOR.execute(() -> {
            final Bitmap preview = decodePreview(activity, source);
            activity.runOnUiThread(() -> {
                if (preview == null) {
                    callback.onFailed();
                } else if (!activity.isFinishing() && !activity.isDestroyed()) {
                    showDialog(activity, source, preview, initialAspect, callback);
                }
            });
        });
    }

    private static void showDialog(Activity activity, Uri source, Bitmap preview,
            float initialAspect, Callback callback) {
        final Dialog dialog = new Dialog(activity,
                android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);

        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setFitsSystemWindows(true);

        final TextView hint = new TextView(activity);
        hint.setText(R.string.ax_qs_image_crop_hint);
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setGravity(Gravity.CENTER);
        final int pad = dp(activity, 16);
        hint.setPadding(pad, pad, pad, 0);
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final QsImageCropView cropView = new QsImageCropView(activity);
        cropView.setBitmap(preview);
        root.addView(cropView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final LinearLayout presets = new LinearLayout(activity);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        final Button free = addPreset(activity, presets, R.string.ax_qs_image_crop_free);
        final Button square = addPreset(activity, presets, R.string.ax_qs_image_crop_square);
        final Button wide = addPreset(activity, presets, R.string.ax_qs_image_crop_wide);
        final Button tall = addPreset(activity, presets, R.string.ax_qs_image_crop_tall);
        final Button[] all = {free, square, wide, tall};
        if (initialAspect == ASPECT_SQUARE) {
            cropView.setAspect(ASPECT_SQUARE);
            selectPreset(all, square);
        } else {
            selectPreset(all, free);
        }
        free.setOnClickListener(v -> {
            cropView.setAspect(QsImageCropView.ASPECT_FREE);
            selectPreset(all, free);
        });
        square.setOnClickListener(v -> {
            cropView.setAspect(ASPECT_SQUARE);
            selectPreset(all, square);
        });
        wide.setOnClickListener(v -> {
            cropView.setAspect(ASPECT_WIDE);
            selectPreset(all, wide);
        });
        tall.setOnClickListener(v -> {
            cropView.setAspect(ASPECT_TALL);
            selectPreset(all, tall);
        });
        root.addView(presets, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.setPadding(pad, 0, pad, pad);
        final Button cancel = newButton(activity, android.R.string.cancel);
        final Button apply = newButton(activity, R.string.ax_qs_image_crop_apply);
        actions.addView(cancel);
        actions.addView(apply);
        root.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cancel.setOnClickListener(v -> dialog.dismiss());
        apply.setOnClickListener(v -> {
            final RectF fractions = cropView.getCropFractions();
            apply.setEnabled(false);
            cancel.setEnabled(false);
            apply.setText(R.string.ax_qs_image_crop_saving);
            EXECUTOR.execute(() -> {
                final Bitmap cropped = decodeCrop(activity, source, fractions);
                activity.runOnUiThread(() -> {
                    dialog.dismiss();
                    if (cropped != null) {
                        callback.onCropped(cropped);
                    } else {
                        callback.onFailed();
                    }
                });
            });
        });

        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.show();
    }

    private static Bitmap decodePreview(Context context, Uri source) {
        try {
            return ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.getContentResolver(), source),
                    (decoder, info, src) -> {
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        final int w = info.getSize().getWidth();
                        final int h = info.getSize().getHeight();
                        final int longest = Math.max(w, h);
                        if (longest > PREVIEW_MAX_SIZE_PX) {
                            final float ratio = (float) PREVIEW_MAX_SIZE_PX / longest;
                            decoder.setTargetSize(Math.max(1, Math.round(w * ratio)),
                                    Math.max(1, Math.round(h * ratio)));
                        }
                    });
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load preview", t);
            return null;
        }
    }

    /**
     * Decodes just the chosen area at the image's native resolution. The size and crop are in the
     * same orientation as the preview, so photos with an EXIF rotation line up.
     */
    private static Bitmap decodeCrop(Context context, Uri source, RectF fractions) {
        final ContentResolver cr = context.getContentResolver();
        try {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(cr, source),
                    (decoder, info, src) -> {
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        final int w = info.getSize().getWidth();
                        final int h = info.getSize().getHeight();
                        final int left = clamp(Math.round(fractions.left * w), 0, w - 1);
                        final int top = clamp(Math.round(fractions.top * h), 0, h - 1);
                        final int right = clamp(Math.round(fractions.right * w), left + 1, w);
                        final int bottom = clamp(Math.round(fractions.bottom * h), top + 1, h);
                        decoder.setCrop(new Rect(left, top, right, bottom));
                    });
        } catch (Throwable t) {
            Log.e(TAG, "Failed to crop image", t);
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Button addPreset(Context context, LinearLayout parent, int textRes) {
        final Button button = newButton(context, textRes);
        parent.addView(button);
        return button;
    }

    private static Button newButton(Context context, int textRes) {
        final Button button = new Button(context, null, android.R.attr.borderlessButtonStyle);
        button.setText(textRes);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        return button;
    }

    private static void selectPreset(Button[] all, Button selected) {
        for (Button b : all) {
            b.setAlpha(b == selected ? 1f : 0.55f);
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
