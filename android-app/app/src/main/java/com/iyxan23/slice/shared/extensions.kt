package com.iyxan23.slice.shared

import android.os.Handler
import android.os.Looper
import io.socket.client.Socket

/**
 * An extension function for `Socket` that runs the ack in the UI thread
 */
fun Socket.utEmit(name: String, args: Array<Any>, ack: (Array<Any?>) -> Unit) {
    emit(name, args) {
        Handler(Looper.getMainLooper()).post { ack(it) }
    }
}

/**
 * An extension function for `Socket` that runs the ack in the UI thread
 */
fun Socket.utOn(name: String, callback: (Array<Any?>) -> Unit) {
    on(name) {
        Handler(Looper.getMainLooper()).post { callback(it) }
    }
}

/**
 * An extension function for `Socket` that runs the ack in the UI thread
 */
fun Socket.utOnce(name: String, callback: (Array<Any?>) -> Unit) {
    once(name) {
        Handler(Looper.getMainLooper()).post { callback(it) }
    }
}
