package com.forevercamaros.charlessummers.escaperoommaster;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.ArrayList;
import java.util.List;

public class PinchZoomGLSurfaceView extends GLSurfaceView {
    private float sizeCoef = 1;

    private List<ScaleChangeListener> listeners = new ArrayList<ScaleChangeListener>();

    ScaleGestureDetector mDetector = new ScaleGestureDetector(getContext(),
            new ScaleDetectorListener());

    public PinchZoomGLSurfaceView(Context context) {
        super(context);
    }

    public PinchZoomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void addScaleChangeListener(ScaleChangeListener listener){
        listeners.add(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    interface ScaleChangeListener {
        void onScaleChange(float scale);
    }


    private class ScaleDetectorListener implements ScaleGestureDetector.OnScaleGestureListener{

        float scaleFocusX = 0;
        float scaleFocusY = 0;

        public boolean onScale(ScaleGestureDetector arg0) {
            float scale = arg0.getScaleFactor() * sizeCoef;

            sizeCoef = scale;

            requestRender();

            for (ScaleChangeListener sl: listeners){
                sl.onScaleChange(scale);
            }

            return true;
        }

        public boolean onScaleBegin(ScaleGestureDetector arg0) {
            invalidate();

            scaleFocusX = arg0.getFocusX();
            scaleFocusY = arg0.getFocusY();

            return true;
        }

        public void onScaleEnd(ScaleGestureDetector arg0) {
            scaleFocusX = 0;
            scaleFocusY = 0;
        }
    }
}
