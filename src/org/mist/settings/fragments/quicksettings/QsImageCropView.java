/*
 * SPDX-FileCopyrightText: MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.fragments.quicksettings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * Shows a preview of the picked image with a draggable crop frame on top. The frame can be moved,
 * resized from its corners and optionally locked to an aspect ratio.
 *
 * The view only holds a screen sized preview. The crop is reported as fractions of the image so
 * the real crop can be taken from the original full resolution pixels.
 */
public class QsImageCropView extends View {

    /** Aspect ratio (width / height) meaning "no lock". */
    public static final float ASPECT_FREE = 0f;

    private static final float INITIAL_SIZE = 0.8f;
    private static final float MIN_SIZE_DP = 72f;
    private static final float HANDLE_TOUCH_DP = 40f;
    private static final float HANDLE_DRAW_DP = 10f;
    private static final float FRAME_PADDING_DP = 16f;

    private static final int NONE = 0;
    private static final int MOVE = 1;
    private static final int TOP_LEFT = 2;
    private static final int TOP_RIGHT = 3;
    private static final int BOTTOM_LEFT = 4;
    private static final int BOTTOM_RIGHT = 5;

    private final float mDensity;
    private final Paint mBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint mDimPaint = new Paint();
    private final Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF mImageRect = new RectF();
    private final RectF mCrop = new RectF();

    private Bitmap mBitmap;
    private float mAspect = ASPECT_FREE;

    private int mDrag = NONE;
    private float mLastX;
    private float mLastY;

    public QsImageCropView(Context context) {
        super(context);
        mDensity = context.getResources().getDisplayMetrics().density;

        mDimPaint.setColor(Color.argb(160, 0, 0, 0));
        mLinePaint.setColor(Color.WHITE);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(2f * mDensity);
        mGridPaint.setColor(Color.argb(110, 255, 255, 255));
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setStrokeWidth(1f * mDensity);
        mHandlePaint.setColor(Color.WHITE);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        layoutImage();
        resetCrop();
        invalidate();
    }

    public void setAspect(float aspect) {
        mAspect = aspect;
        resetCrop();
        invalidate();
    }

    /** The crop as fractions (0..1) of the image width and height. */
    public RectF getCropFractions() {
        if (mImageRect.width() <= 0 || mImageRect.height() <= 0) {
            return new RectF(0f, 0f, 1f, 1f);
        }
        return new RectF(
                clamp01((mCrop.left - mImageRect.left) / mImageRect.width()),
                clamp01((mCrop.top - mImageRect.top) / mImageRect.height()),
                clamp01((mCrop.right - mImageRect.left) / mImageRect.width()),
                clamp01((mCrop.bottom - mImageRect.top) / mImageRect.height()));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutImage();
        resetCrop();
    }

    private void layoutImage() {
        if (mBitmap == null || getWidth() == 0 || getHeight() == 0) return;
        final float pad = FRAME_PADDING_DP * mDensity;
        final float availW = getWidth() - pad * 2;
        final float availH = getHeight() - pad * 2;
        final float scale = Math.min(availW / mBitmap.getWidth(), availH / mBitmap.getHeight());
        final float w = mBitmap.getWidth() * scale;
        final float h = mBitmap.getHeight() * scale;
        final float left = (getWidth() - w) / 2f;
        final float top = (getHeight() - h) / 2f;
        mImageRect.set(left, top, left + w, top + h);
    }

    private void resetCrop() {
        if (mImageRect.isEmpty()) return;
        float w = mImageRect.width() * INITIAL_SIZE;
        float h = mImageRect.height() * INITIAL_SIZE;
        if (mAspect > 0f) {
            if (w / h > mAspect) {
                w = h * mAspect;
            } else {
                h = w / mAspect;
            }
        }
        final float cx = mImageRect.centerX();
        final float cy = mImageRect.centerY();
        mCrop.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null || mImageRect.isEmpty()) return;
        canvas.drawBitmap(mBitmap, null, mImageRect, mBitmapPaint);

        // Dim everything outside of the crop frame.
        canvas.drawRect(mImageRect.left, mImageRect.top, mImageRect.right, mCrop.top, mDimPaint);
        canvas.drawRect(mImageRect.left, mCrop.bottom, mImageRect.right, mImageRect.bottom,
                mDimPaint);
        canvas.drawRect(mImageRect.left, mCrop.top, mCrop.left, mCrop.bottom, mDimPaint);
        canvas.drawRect(mCrop.right, mCrop.top, mImageRect.right, mCrop.bottom, mDimPaint);

        canvas.drawRect(mCrop, mLinePaint);
        final float thirdW = mCrop.width() / 3f;
        final float thirdH = mCrop.height() / 3f;
        for (int i = 1; i <= 2; i++) {
            canvas.drawLine(mCrop.left + thirdW * i, mCrop.top, mCrop.left + thirdW * i,
                    mCrop.bottom, mGridPaint);
            canvas.drawLine(mCrop.left, mCrop.top + thirdH * i, mCrop.right,
                    mCrop.top + thirdH * i, mGridPaint);
        }

        final float r = HANDLE_DRAW_DP * mDensity;
        canvas.drawCircle(mCrop.left, mCrop.top, r, mHandlePaint);
        canvas.drawCircle(mCrop.right, mCrop.top, r, mHandlePaint);
        canvas.drawCircle(mCrop.left, mCrop.bottom, r, mHandlePaint);
        canvas.drawCircle(mCrop.right, mCrop.bottom, r, mHandlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDrag = hitTest(event.getX(), event.getY());
                mLastX = event.getX();
                mLastY = event.getY();
                if (mDrag != NONE) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return mDrag != NONE;
            case MotionEvent.ACTION_MOVE:
                if (mDrag == NONE) return false;
                final float dx = event.getX() - mLastX;
                final float dy = event.getY() - mLastY;
                mLastX = event.getX();
                mLastY = event.getY();
                if (mDrag == MOVE) {
                    move(dx, dy);
                } else {
                    resize(mDrag, dx, dy);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mDrag = NONE;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private int hitTest(float x, float y) {
        final float touch = HANDLE_TOUCH_DP * mDensity;
        if (near(x, y, mCrop.left, mCrop.top, touch)) return TOP_LEFT;
        if (near(x, y, mCrop.right, mCrop.top, touch)) return TOP_RIGHT;
        if (near(x, y, mCrop.left, mCrop.bottom, touch)) return BOTTOM_LEFT;
        if (near(x, y, mCrop.right, mCrop.bottom, touch)) return BOTTOM_RIGHT;
        return mCrop.contains(x, y) ? MOVE : NONE;
    }

    private static boolean near(float x, float y, float px, float py, float radius) {
        return Math.abs(x - px) <= radius && Math.abs(y - py) <= radius;
    }

    private void move(float dx, float dy) {
        final float nx = Math.max(mImageRect.left - mCrop.left,
                Math.min(dx, mImageRect.right - mCrop.right));
        final float ny = Math.max(mImageRect.top - mCrop.top,
                Math.min(dy, mImageRect.bottom - mCrop.bottom));
        mCrop.offset(nx, ny);
    }

    private void resize(int corner, float dx, float dy) {
        final boolean left = corner == TOP_LEFT || corner == BOTTOM_LEFT;
        final boolean top = corner == TOP_LEFT || corner == TOP_RIGHT;
        // The opposite corner stays where it is.
        final float anchorX = left ? mCrop.right : mCrop.left;
        final float anchorY = top ? mCrop.bottom : mCrop.top;
        final float signX = left ? -1f : 1f;
        final float signY = top ? -1f : 1f;

        final float movingX = (left ? mCrop.left : mCrop.right) + dx;
        final float movingY = (top ? mCrop.top : mCrop.bottom) + dy;

        final float min = MIN_SIZE_DP * mDensity;
        final float maxW = signX < 0 ? anchorX - mImageRect.left : mImageRect.right - anchorX;
        final float maxH = signY < 0 ? anchorY - mImageRect.top : mImageRect.bottom - anchorY;

        float w = Math.max(min, Math.abs(movingX - anchorX));
        float h = Math.max(min, Math.abs(movingY - anchorY));

        if (mAspect > 0f) {
            if (w / h > mAspect) {
                w = h * mAspect;
            } else {
                h = w / mAspect;
            }
            // Keep the ratio when one side runs into the edge of the image.
            if (w > maxW) {
                w = maxW;
                h = w / mAspect;
            }
            if (h > maxH) {
                h = maxH;
                w = h * mAspect;
            }
        } else {
            w = Math.min(w, maxW);
            h = Math.min(h, maxH);
        }

        final float endX = anchorX + signX * w;
        final float endY = anchorY + signY * h;
        mCrop.set(Math.min(anchorX, endX), Math.min(anchorY, endY),
                Math.max(anchorX, endX), Math.max(anchorY, endY));
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
