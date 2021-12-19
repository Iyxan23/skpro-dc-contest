package com.iyxan23.slice.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.iyxan23.slice.R
import org.webrtc.PeerConnectionFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize webrtc
        PeerConnectionFactory.initializeAndroidGlobals(applicationContext, true)
    }
}