package com.forevercamaros.charlessummers.escaperoommaster;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class PinchZoomGLSurfaceView extends GLSurfaceView {
    private float sizeCoef = 1;

    private List<ScaleChangeListener> listeners = new ArrayList<ScaleChangeListener>();

    ScaleGestureDetector mDetector = new ScaleGestureDetector(getContext(),
            new ScaleDetectorListener(this));
    GestureDetector mDetector2 = new GestureDetector(getContext(),
            new ScaleDetectorListener(this));

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
        //Zoom Temporarily Disabled
        /*mDetector.onTouchEvent(event);
        mDetector2.onTouchEvent(event);*/
        return super.onTouchEvent(event);
    }

    interface ScaleChangeListener {
        void onScaleChange(float scale);
    }


    private class ScaleDetectorListener implements GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener{

        private View view;
        private float scaleFactor = 1;

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            float newX = view.getX();
            float newY = view.getY();
            /*if(!inScale){
                newX -= x;
                newY -= y;
            }*/
            WindowManager wm = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
            Display d = wm.getDefaultDisplay();
            Point p = new Point();
            d.getSize(p);

            if (newX > (view.getWidth() * scaleFactor - p.x) / 2){
                newX = (view.getWidth() * scaleFactor - p.x) / 2;
            } else if (newX < -((view.getWidth() * scaleFactor - p.x) / 2)){
                newX = -((view.getWidth() * scaleFactor - p.x) / 2);
            }

            if (newY > (view.getHeight() * scaleFactor - p.y) / 2){
                newY = (view.getHeight() * scaleFactor - p.y) / 2;
            } else if (newY < -((view.getHeight() * scaleFactor - p.y) / 2)){
                newY = -((view.getHeight() * scaleFactor - p.y) / 2);
            }

            view.setX(newX);
            view.setY(newY);

            Log.d("PinchZoom", "onScroll: " + Float.toString(view.getX()) + "," + Float.toString(view.getY()));

            return true;
        }

        public ScaleDetectorListener(View _view){
            view=_view;
        }
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = (scaleFactor < 1 ? 1 : scaleFactor); // prevent our view from becoming too small //
            scaleFactor = ((float)((int)(scaleFactor * 100))) / 100; // Change precision to help with jitter when user just rests their fingers //
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);


            requestRender();

            for (ScaleChangeListener sl: listeners){
                sl.onScaleChange(scaleFactor);
            }

            return true;
        }

        public boolean onScaleBegin(ScaleGestureDetector arg0) {
            invalidate();

            return true;
        }

        public void onScaleEnd(ScaleGestureDetector arg0) {
        }

    }
}
