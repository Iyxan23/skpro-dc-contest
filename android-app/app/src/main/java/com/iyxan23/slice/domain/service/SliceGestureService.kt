package com.iyxan23.slice.domain.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.iyxan23.slice.domain.models.SliceGestureMessage

const val TAG = "SliceGestureService"
const val DISPATCH_GESTURE_ACTION = "slice_dispatch_gesture"

/**
 * This service is used to dispatch gestures to the screen
 *
 * Sadly this only works for API 26+, If anybody is wondering why TeamViewer works on older
 * versions, it is most likely because they made a deal with android phone manufacturers to
 * include a specific API for them to be able to control the screen, hence that's why they
 * don't support all devices.
 *
 * Source: https://stackoverflow.com/questions/59278085/how-perform-a-drag-based-in-x-y-mouse-coordinates-on-android-using-accessibili
 */
class SliceGestureService : AccessibilityService() {
    inner class SliceBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "onReceive: Received a gesture dispatch broadcast!")

            // retrieve the actions we needed to perform
            val actions = intent.getParcelableArrayListExtra<SliceGestureMessage>("gestures")
            val builder = GestureDescription.Builder()
            var stroke: GestureDescription.StrokeDescription? = null

            var previousPositionX = 0f
            var previousPositionY = 0f

            // loop over each actions and build them into a gesture description
            actions?.forEach { action ->
                when (action) {
                    // when it's hold, we set the path to the specified x and y and set
                    // willContinue to be true (let it hold)
                    is SliceGestureMessage.Hold -> {
                        Log.d(TAG, "onReceive: Hold: $action")
                        stroke =
                            GestureDescription.StrokeDescription(
                                Path().apply { moveTo(action.x.toFloat(), action.y.toFloat()) },
                                0, 1, true
                            )

                        previousPositionX = action.x.toFloat()
                        previousPositionY = action.y.toFloat()
                    }

                    // when it's move, we want to start the pointer to the previous position, and
                    // move it to the specified x and y position (and set willContinue to be true
                    // because we haven't reached Release yet)
                    is SliceGestureMessage.Move -> {
                        Log.d(TAG, "onReceive: Move: $action")
                        stroke = stroke?.continueStroke(
                            Path().apply {
                                moveTo(previousPositionX, previousPositionY)
                                lineTo(action.x.toFloat(), action.y.toFloat())
                            }, 0, action.duration, true
                        )
                    }

                    // when it's release, we wanted to just set willContinue to be false so that it
                    // will just release the pointer
                    SliceGestureMessage.Release -> {
                        Log.d(TAG, "onReceive: Release $action")
                        stroke = stroke?.continueStroke(
                            Path().apply { moveTo(previousPositionX, previousPositionY) },
                            0, 1, false
                        )
                    }
                }
            }

            // then add the stroke to the builder
            stroke?.let { builder.addStroke(it) }

            // and finally we dispatch the gesture
            dispatchGesture(builder.build(),
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        super.onCompleted(gestureDescription)

                        Log.i(TAG, "onCompleted: dispatched successfully")
                    }
                },
                null
            )
        }
    }

    override fun onCreate() {
        super.onCreate()

        // register the broadcast receiver
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(
                SliceBroadcastReceiver(),
                IntentFilter().apply { addAction(DISPATCH_GESTURE_ACTION) }
            )

        Log.i(TAG, "onCreate: Service created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) { /* don't need this */ }
    override fun onInterrupt() { Log.d(TAG, "onInterrupt() called") }
}