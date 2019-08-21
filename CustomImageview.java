package com.js.home.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import Js.baselibrary.utils.LogUtils;
import Js.baselibrary.utils.ToastUtils;

public class CustomImageview extends ImageView implements ScaleGestureDetector.OnScaleGestureListener, ViewTreeObserver.OnGlobalLayoutListener {
    private Matrix matrix = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    public float borderWidth;
    public PointF startPoint = new PointF();
    private float currentX;
    private float currentY;
    private boolean doubleClicked;

    private int mode;
    private int ZOOM_MODE = 1;
    private int DRAG_MODE = 2;

    private boolean once = true;
    private float MAX_SCALE = 4.0f;
    private float MIN_SCALE = 0.8f;
    private float[] matrixValue = new float[9];
    private float initScale;

    public CustomImageview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomImageview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.MATRIX);
        scaleGestureDetector = new ScaleGestureDetector(context, this);
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                LogUtils.e("onDoubleTap");
                scaleDoubleAction(e.getX(), e.getY());
                return super.onDoubleTap(e);
            }
        });
    }


    public CustomImageview(Context context) {
        this(context, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleGestureDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //解决和外层父view滑动冲突的问题，设置父类拦截或不拦截touch事件，默认又父类拦截
                if ((ev.getX() <= getWidth() - borderWidth) && ev.getX() > borderWidth) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                startPoint.set(ev.getX(), ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:

                // 当前是放大 且是拖拽模式的时候才可以进行拖拽操作
                if (mode == DRAG_MODE && getScale() > initScale) {
                    float dx = ev.getX() - startPoint.x;
                    float dy = ev.getY() - startPoint.y;
                    matrix.postTranslate(dx, dy);
                    setImageMatrix(matrix);
                    startPoint.set(ev.getX(), ev.getY());
                }

                break;
        }
        return true;
    }


    //双击
    public boolean scaleDoubleAction(float x, float y) {
        float scaleFactor = scaleGestureDetector.getScaleFactor();
        //如果是放大操作，限制最大放大值
//        if (getScale()*2 >MAX_SCALE){
//            return  true;
//        }

        //如果是放大模式，双击恢复原貌
        if (getScale() > initScale) {
            backOrigin();
            return true;
        }
        if (doubleClicked) {
            backOrigin();
            return true;
        }
        mode = DRAG_MODE;
        currentX = x;
        currentY = y;
        matrix.postScale(scaleFactor * 2, scaleFactor * 2, x, y);
        setImageMatrix(matrix);
        doubleClicked = true;
        return true;
    }

    public boolean scaleAction() {
        float scaleFactor = scaleGestureDetector.getScaleFactor();
        //如果是放大操作，限制最大放大值
//        if (scaleFactor>1.0f && getScale()>=MAX_SCALE){
//            return  true;
//        }
        if (currentX != 0f && currentY != 0f) {
            if (getScale() >= initScale) {
                matrix.postScale(scaleFactor, scaleFactor, currentX, currentY);
            } else {
                matrix.postScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);
            }
        } else {
            matrix.postScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);
        }
        setImageMatrix(matrix);
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return scaleAction();
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        LogUtils.e("onScaleBegin");
        mode = ZOOM_MODE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        LogUtils.e("onScaleEnd" + getScale());
        LogUtils.e("initScale" + initScale);
        if (getScale() < initScale) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    backOrigin();
                    mode = DRAG_MODE;
                }
            }, 200);
        } else {
            //放大改变为拖拽模式后 防止和 拖拽冲突造成界面滑动，加200毫秒延时
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    mode = DRAG_MODE;
                }
            }, 200);
        }

    }


    //恢复成初始状态
    public void backOrigin() {
        doubleClicked = false;
        matrix.reset();
        setImageMatrix(matrix);
        initScale();
    }


    //获取当前图片的缩放比例
    public float getScale() {
        matrix.getValues(matrixValue);
        return matrixValue[Matrix.MSCALE_X];
    }


    //获取图片宽高，并设置图片显示位置和大小
    @Override
    public void onGlobalLayout() {
        if (!once) {
            return;
        }
        borderWidth = getWidth() / 6;
        Drawable drawable = getDrawable();
        if (drawable == null) return;
        LogUtils.e("onGlobalLayout");
        //控件宽高
        int width = getWidth();
        int height = getHeight();

        //图片宽高
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        //进行等比例缩放
        float scale = 1.0f;
        //如果图片的宽或者高大于控件的，缩放至屏幕的宽高
        if (intrinsicWidth > width && intrinsicHeight < height) {
            scale = width / intrinsicWidth;
        }
        if (intrinsicHeight > height && intrinsicWidth < intrinsicWidth) {
            scale = height / intrinsicHeight;
        }
        //如果宽高 都大于控件的
        if (intrinsicHeight > height && intrinsicWidth > width) {
            scale = Math.min(height / intrinsicHeight, width / intrinsicWidth);
        }

        if (intrinsicWidth < width && intrinsicHeight < height) {
            scale = width / intrinsicWidth;
        }
        initScale = scale;
        LogUtils.e(initScale + "初始缩放值");
        matrix.postTranslate((width - intrinsicWidth) / 2, (height - intrinsicHeight) / 2);
        matrix.postScale(scale, scale, width / 2, height / 2);
        setImageMatrix(matrix);
        once = false;
    }

    //初始化 图像的位置，图像宽度和屏幕同宽
    public void initScale() {
        Drawable drawable = getDrawable();
        if (drawable == null) return;
        //控件宽高
        int width = getWidth();
        int height = getHeight();

        //图片宽高
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        //进行等比例缩放
        float scale = 1.0f;
        //如果图片的宽或者高大于控件的，缩放至屏幕的宽高
        if (intrinsicWidth > width && intrinsicHeight < height) {
            scale = width / intrinsicWidth;
        }
        if (intrinsicHeight > height && intrinsicWidth < intrinsicWidth) {
            scale = height / intrinsicHeight;
        }
        //如果宽高 都大于控件的
        if (intrinsicHeight > height && intrinsicWidth > width) {
            scale = Math.min(height / intrinsicHeight, width / intrinsicWidth);
        }

        if (intrinsicWidth < width && intrinsicHeight < height) {
            scale = width / intrinsicWidth;
        }
        initScale = scale;
        LogUtils.e(initScale + "初始缩放值");
        matrix.postTranslate((width - intrinsicWidth) / 2, (height - intrinsicHeight) / 2);
        matrix.postScale(scale, scale, width / 2, height / 2);
        setImageMatrix(matrix);
    }

}
