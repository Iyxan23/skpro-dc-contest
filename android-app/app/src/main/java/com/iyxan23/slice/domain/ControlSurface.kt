package com.iyxan23.slice.domain

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceView

/**
 * This custom view is used to display the RTMP video stream from remote and detect touch
 * gestures
 */
class ControlSurface(context: Context) : SurfaceView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // todo: do things with touches here
        return true;
    }
}