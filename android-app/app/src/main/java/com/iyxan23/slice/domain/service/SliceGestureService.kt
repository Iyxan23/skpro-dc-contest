package com.iyxan23.slice.domain.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

const val TAG = "SliceGestureService"

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
// todo: communicate with the app n stuff
class SliceGestureService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) { /* don't need this */ }
    override fun onInterrupt() { Log.d(TAG, "onInterrupt() called") }
}