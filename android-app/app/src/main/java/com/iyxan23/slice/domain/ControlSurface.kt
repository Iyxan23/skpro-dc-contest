package com.iyxan23.slice.domain

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import com.iyxan23.slice.domain.models.SliceGestureMessage

/**
 * How many times should we sample (retrieve touch events) per second?
 *
 * 10 sample size = 10 samples per second
 *
 * The higher the number the smoother and more cpu-heavy it gets
 */
const val touchSampleSize = 5
const val touchSampleDelay = 1 / touchSampleSize

/**
 * This custom view is used to display the RTMP video stream from remote and detect touch
 * gestures
 */
// todo: change this back to SurfaceView when we got to playing around with streaming video
class ControlSurface : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * The last time the last sample got sampled, this is used to comply with sampleSize.
     *
     * MUST USE `SystemClock.uptimeMillis()`
     */
    private var lastSample = 0

    private var curMessage: SliceGestureMessage? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

        // check if we're allowed to sample (last sample was above the sample delay)
        if (SystemClock.uptimeMillis() - lastSample < touchSampleDelay) return true

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.i(TAG, "onTouchEvent: down at x: ${event.rawX} y: ${event.rawY}")
                curMessage = SliceGestureMessage.Hold(event.rawX.toInt(), event.rawY.toInt())
                invalidate()
                true
            }

            MotionEvent.ACTION_MOVE -> {
                Log.i(TAG, "onTouchEvent: move to x: ${event.rawX} y: ${event.rawY}")
                curMessage = SliceGestureMessage.Move(
                    event.rawX.toInt(),
                    event.rawY.toInt(),
                    100L
                )
                invalidate()
                true
            }

            MotionEvent.ACTION_UP -> {
                Log.i(TAG, "onTouchEvent: up at x: ${event.rawX} y: ${event.rawY}")
                curMessage = SliceGestureMessage.Release
                invalidate()
                true
            }

            else -> false
        }
    }

    // vvv testing code to make sure it works vvv

    private val holdPaint: Paint = Paint().apply {
        color = 0xFFFF0000.toInt()
    }

    private val linePaint: Paint = Paint().apply {
        color = 0xFF000000.toInt()
        strokeWidth = 5f
    }

    private val releasePaint: Paint = Paint().apply {
        color = 0xFF0000FF.toInt()
    }

    private var previousPositionX = 0
    private var previousPositionY = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        curMessage?.let {
            when (it) {
                is SliceGestureMessage.Hold -> {
                    previousPositionX = it.x
                    previousPositionY = it.y
                    canvas.drawCircle(it.x.toFloat(), it.y.toFloat(), 10f, holdPaint)
                }
                is SliceGestureMessage.Move -> {
                    canvas.drawLine(
                        previousPositionX.toFloat(),
                        previousPositionY.toFloat(),
                        it.x.toFloat(),
                        it.y.toFloat(),
                        linePaint
                    )

                    previousPositionX = it.x
                    previousPositionY = it.y
                }
                SliceGestureMessage.Release -> {
                    canvas.drawCircle(
                        previousPositionX.toFloat(),
                        previousPositionY.toFloat(),
                        10f,
                        releasePaint
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "ControlSurface"
    }
}