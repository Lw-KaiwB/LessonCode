package com.kb.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.AnimatorRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.kb.R;
import com.kb.util.Utils;

public class ScalableImageView extends View implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
    private String TAG = this.getClass().getSimpleName();

    private Paint mPaint;
    private Bitmap mBitmap;
    private GestureDetector mGestureDetector;
    private final int IMAGE_SIZE = 400;
    private float offsetX;
    private float offsetY;
    private float imageWidth;
    private float imageHeight;
    private float smallScale;
    private float bigScale;
    private boolean isBig = false;

    public ScalableImageView(Context context) {
        this(context, null);
    }

    public ScalableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mBitmap = Utils.getAvatar(getResources(), R.drawable.aodamiao, IMAGE_SIZE);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isTrack = mGestureDetector.onTouchEvent(event);
        if (!isTrack) {
            return super.onTouchEvent(event);
        }
        return isTrack;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        isBig = !isBig;
        if (isBig) {

        } else {

        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        offsetX = (getWidth() - mBitmap.getWidth()) / 2;
        offsetY = (getHeight() - mBitmap.getHeight()) / 2;
        imageWidth = mBitmap.getWidth();
        imageHeight = mBitmap.getHeight();

        Log.e(TAG, "imageWidth/imageHeight=" + (imageWidth / imageHeight) + " getWidth()/getHeight()=" + ((float) getWidth() / getHeight()));
        if (imageWidth / imageHeight > (float) getWidth() / getHeight()) {
            smallScale = getWidth() / imageWidth;
            bigScale = getHeight() / imageHeight;
            Log.e(TAG, "smallScale=" + smallScale + " bigScale=" + bigScale);
        } else {
            smallScale = getHeight() / imageHeight;
            bigScale = getWidth() / imageWidth;
            Log.e(TAG, "smallScale1=" + smallScale + " bigScale1=" + bigScale);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(offsetX, offsetY);
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }
}
