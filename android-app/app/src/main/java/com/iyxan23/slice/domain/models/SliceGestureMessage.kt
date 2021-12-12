package com.iyxan23.slice.domain.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A model that's used to tell SliceGestureService what to do
 */
sealed class SliceGestureMessage : Parcelable {
    /**
     * Starts the gesture at the defined location, or in simpler terms, your finger starts touching
     * the screen
     */
    @Parcelize data class Hold(val x: Int, val y: Int) : SliceGestureMessage()

    /**
     * Move the gesture to the defined location with a specific duration, or your finger starts
     * moving around the screen
     */
    @Parcelize data class Move(
        val x: Int,
        val y: Int,

        /** Duration, in milliseconds */
        val duration: Long
    ) : SliceGestureMessage()

    /**
     * Stops the gesture, or you elevates your finger upward to stop touching the screen
     */
    @Parcelize object Release : SliceGestureMessage()
}