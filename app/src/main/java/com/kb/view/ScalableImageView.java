package com.kb.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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
import android.widget.OverScroller;

import com.kb.R;
import com.kb.util.Utils;

public class ScalableImageView extends View implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener, Runnable {
    private String TAG = this.getClass().getSimpleName();

    private Paint mPaint;
    private Bitmap mBitmap;
    private GestureDetector mGestureDetector;
    private final int IMAGE_SIZE = 400;
    private final int SCALE_OVER_FACTOR = 2;
    private float offsetX;
    private float offsetY;
    private float orignalOffsetX;
    private float orignalOffsetY;
    private float imageWidth;
    private float imageHeight;
    private float smallScale;
    private float bigScale;
    private boolean isBig = false;
    private float scalingFraction;
    private ObjectAnimator scaleAnimator;
    private OverScroller mOverScaller;

    public ScalableImageView(Context context) {
        this(context, null);
    }

    public ScalableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mBitmap = Utils.getAvatar(getResources(), R.drawable.aodamiao, IMAGE_SIZE);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGestureDetector = new GestureDetector(context, this);
        mGestureDetector.setOnDoubleTapListener(this);
        mOverScaller = new OverScroller(context);
    }

    public float getScalingFraction() {
        return scalingFraction;
    }

    public void setScalingFraction(float scalingFraction) {
        this.scalingFraction = scalingFraction;
        invalidate();
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
        if (isBig) {
            offsetX -= distanceX;
            offsetY -= distanceY;

            offsetX = Math.min(offsetX, (imageWidth * bigScale - getWidth()) / 2);
            offsetX = Math.max(offsetX, -(imageWidth * bigScale - getWidth()) / 2);

            offsetY = Math.min(offsetY, (imageHeight * bigScale - getHeight()) / 2);
            offsetY = Math.max(offsetY, -(imageHeight * bigScale - getHeight()) / 2);
            invalidate();
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mOverScaller.fling((int) offsetX, (int) offsetY, (int) velocityX, (int) velocityY,
                (int) (getWidth() - imageWidth * bigScale) / 2, (int) (imageWidth * bigScale - getWidth()) / 2,
                (int) (getHeight() - imageHeight * bigScale) / 2, (int) (imageHeight * bigScale - getHeight()) / 2,
                0, 0);
        postOnAnimation(this);
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
            offsetX = -(e.getX()-getWidth()/2);
            offsetY = -(e.getY() - getHeight()/2);
            getScaleAnimator().start();
        } else {
            getScaleAnimator().reverse();
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

        imageWidth = mBitmap.getWidth();
        imageHeight = mBitmap.getHeight();
        orignalOffsetX = (getWidth() - imageWidth) / 2;
        orignalOffsetY = (getHeight() - imageHeight) / 2;

        if (imageWidth / imageHeight > (float) getWidth() / getHeight()) {
            smallScale = getWidth() / imageWidth;
            bigScale = getHeight() / imageHeight * SCALE_OVER_FACTOR;
        } else {
            smallScale = getHeight() / imageHeight;
            bigScale = getWidth() / imageWidth * SCALE_OVER_FACTOR;
        }
    }

    private ObjectAnimator getScaleAnimator() {
        if (scaleAnimator == null) {
            scaleAnimator = ObjectAnimator.ofFloat(this, "scalingFraction", 0, 1);
            scaleAnimator.setDuration(300);
            scaleAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation, boolean isReverse) {
                    if (isReverse)
                        offsetX = offsetY = 0;
                }
            });
        }
        return scaleAnimator;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = smallScale + (bigScale - smallScale) * scalingFraction;
        canvas.translate(offsetX * scalingFraction, offsetY * scalingFraction);
        canvas.scale(scale, scale, getWidth() / 2, getHeight() / 2);
        canvas.translate(orignalOffsetX, orignalOffsetY);
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }

    @Override
    public void run() {
        if (mOverScaller.computeScrollOffset()) {
            offsetY = mOverScaller.getCurrY();
            offsetX = mOverScaller.getCurrX();
            invalidate();
            postOnAnimation(this);
        }

    }
}
