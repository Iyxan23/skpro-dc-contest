package com.iyxan23.slice.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.iyxan23.slice.R
import org.webrtc.Logging
import org.webrtc.PeerConnectionFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize webrtc
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .setEnableInternalTracer(true)
                .setInjectableLogger({ name, severity, msg ->
                    when (severity!!) {
                        Logging.Severity.LS_VERBOSE -> Log.v("WebRTC $name", msg)
                        Logging.Severity.LS_INFO -> Log.i("WebRTC $name", msg)
                        Logging.Severity.LS_WARNING -> Log.w("WebRTC $name", msg)
                        Logging.Severity.LS_ERROR -> Log.e("WebRTC $name", msg)
                        else -> {}
                    }
                }, Logging.Severity.LS_VERBOSE)
                .createInitializationOptions()
        )
    }
}