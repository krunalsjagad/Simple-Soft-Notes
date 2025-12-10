package com.example.softnotesandcanvas.ui.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class CanvasView extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mPaint;
    private Paint mBitmapPaint;

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    // This will hold a bitmap that is loaded before the view is measured
    private Bitmap bitmapToLoad = null;

    private boolean isErasing = false;
    private int mCurrentColor = Color.BLACK;
    private int mCurrentStrokeWidth = 8;
    private int mEraserStrokeWidth = 50;


    public CanvasView(Context context) {
        super(context);
        init();
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(mCurrentColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mCurrentStrokeWidth);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            // Only create a new bitmap if one doesn't exist
            // (e.g., on first layout or orientation change)
            if (mBitmap == null) {
                mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);

                // Check if we have a bitmap pending from loadBitmap()
                if (bitmapToLoad != null) {
                    mCanvas.drawBitmap(bitmapToLoad, 0, 0, null);
                    bitmapToLoad = null; // We've used it
                } else {
                    // This is a new canvas, so fill with white
                    mCanvas.drawColor(Color.WHITE);
                }
            }
        }
    } //

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        }
        canvas.drawPath(mPath, mPaint);
    }

    private void touchStart(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
        // Commit the path to our bitmap
        mCanvas.drawPath(mPath, mPaint);
        // Kill this path
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }
        return true;
    }

    // --- Public Methods for Activity to call ---

    public void setPenMode() {
        isErasing = false;
        mPaint.setColor(mCurrentColor);
        mPaint.setStrokeWidth(mCurrentStrokeWidth);
        mPaint.setXfermode(null); // Standard drawing mode
    }

    public void setEraserMode() {
        isErasing = true;
        mPaint.setColor(Color.WHITE);
        // Set the paint to "clear" the canvas
        mPaint.setXfermode(null);
        mPaint.setStrokeWidth(mEraserStrokeWidth);
    }

    public void clearCanvas() {
        // Clear the bitmap by drawing white over it
        mCanvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_OVER);
        invalidate();
    }

    public void loadBitmap(Bitmap bitmap) {
        // Store the bitmap to be loaded.
        this.bitmapToLoad = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // If mCanvas is already set up, we can draw on it now.
        // Otherwise, onSizeChanged will handle drawing it onto the new bitmap.
        if (mCanvas != null) {
            // Clear the canvas and draw the new bitmap
            mCanvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_OVER);
            mCanvas.drawBitmap(this.bitmapToLoad, 0, 0, null);
            this.bitmapToLoad = null; // It's been loaded
        }

        invalidate();
    }

    public Bitmap getBitmap() {
        // This returns the bitmap with the drawing on it
        return mBitmap;
    }
}