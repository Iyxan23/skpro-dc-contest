package com.iyxan23.slice.domain.input

import android.annotation.SuppressLint
import android.hardware.input.InputManager
import android.os.SystemClock
import android.view.InputEvent
import android.view.KeyEvent
import kotlin.Throws
import android.view.MotionEvent
import androidx.core.view.InputDeviceCompat
import java.lang.reflect.InvocationTargetException

/**
 * Modified from:
 * https://github.com/omerjerk/RemoteDroid/blob/master/app/src/main/java/in/omerjerk/remotedroid/app/EventInput.java
 *
 * Class to create seamless input/touch events on your Android device without root
 */
// okay nevermind this is impossible, we will need this app to be installed as a system app to be
// able to use this technique
@SuppressLint("DiscouragedPrivateApi") // <- ha
object EventInput {
    // retrieves the injectInputEvent method to inject input events
    private val injectInputEvent =
        InputManager::class.java
            .getMethod("injectInputEvent", InputEvent::class.java, Integer.TYPE)

    // retrieves the InputManager from reflection
    private val inputManager =
        InputManager::class.java
            .getDeclaredMethod("getInstance")
            .invoke(null) as InputManager

    enum class TouchAction {
        FINGER_DOWN,
        FINGER_UP,
        FINGER_MOVE,
    }

    /**
     * Injects a touch event to the touchscreen
     */
    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    fun injectTouch(
        action: TouchAction,
        x: Float, y: Float,
        pressure: Float
    ): Boolean {
        return injectMotionEvent(
            InputDeviceCompat.SOURCE_TOUCHSCREEN,
            when (action) {
                TouchAction.FINGER_DOWN -> 0
                TouchAction.FINGER_UP -> 1
                TouchAction.FINGER_MOVE -> 2
            },
            SystemClock.uptimeMillis(),
            x, y,
            pressure
        )
    }

    /**
     * Injects a motion to the specified input source
     */
    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    fun injectMotionEvent(
        inputSource: Int,
        action: Int,
        time: Long,
        x: Float,
        y: Float,
        pressure: Float
    ): Boolean {
        val event = MotionEvent.obtain(
            time,
            time,
            action,
            x, y,
            pressure,
            1.0f,
            0,
            1.0f,
            1.0f,
            0,
            0
        )

        event.source = inputSource

        return injectInputEvent.invoke(inputManager, event, 1) as Boolean
    }

    /**
     * Injects a key event to the device
     */
    @Throws(InvocationTargetException::class, IllegalAccessException::class)
    private fun injectKeyEvent(event: KeyEvent) {
        injectInputEvent.invoke(inputManager, event, 0)
    }

    init {
        // make the obtain function accessible :>
        MotionEvent::class.java.getDeclaredMethod("obtain").isAccessible = true
    }
}