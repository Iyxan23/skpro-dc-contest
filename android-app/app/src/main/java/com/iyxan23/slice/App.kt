package com.iyxan23.slice

import android.app.Application
import com.iyxan23.slice.shared.DEBUG_URL
import com.iyxan23.slice.shared.SERVER_URL
import io.socket.client.IO
import io.socket.client.Socket

class App : Application() {
    /**
     * The socket we use to communicate with the server
     */
    val socket: Socket = IO.socket(if (BuildConfig.DEBUG) DEBUG_URL else SERVER_URL)
}