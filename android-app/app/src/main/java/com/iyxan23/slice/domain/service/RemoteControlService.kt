package com.iyxan23.slice.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.domain.models.response.GenericResponse
import com.iyxan23.slice.shared.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.webrtc.*

class RemoteControlService : Service() {
    companion object {
        private const val CHANNEL_ID = "SliceRemoteControlServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "RemoteControlService"
    }

    private lateinit var connection: PeerConnection
    private lateinit var screenVideoTrack: VideoTrack
    private lateinit var screenVideoSource: VideoSource
    private val socket by lazy { (application as App).socket }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        socket // to retrieve the socket

        val offer = intent.getStringExtra("controller_offer")!!
        val mediaProjectionToken = intent.getParcelableExtra<Intent>("media_projection_token")!!

        Log.d(TAG, "onStartCommand: Service started with offer: $offer")

        createNotificationChannel()

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Slice Remote Control")
            .setContentText("Connecting")
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // do WebRTC things

        // my gosh google's webrtc lib is so trash, i can't even find any documentation online
        // NOT A SINGLE BIT, except for the source code
        val factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options().apply { disableEncryption = false })
            .createPeerConnectionFactory()

        connection = factory
            .createPeerConnection(iceServers, object : LogPeerConnectionObserver(TAG) {
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    super.onIceGatheringChange(state)

                    // check if we've finished gathering ICE candidates
                    if (state == PeerConnection.IceGatheringState.COMPLETE) {
                        Log.d(TAG, "onIceGatheringChange: Ice gathering complete! send answer!")

                        // ice candidates gathered! send this answer to the server
                        socket.emit(SOCKET_SET_SDP, arrayOf(connection.localDescription.description)) {
                            if (it[0] == null) {
                                Log.e(TAG, "onCreateSuccess: Invalid ack for set ice: ${it.toList()}")
                                return@emit
                            }

                            when (val response = Json.decodeFromString<GenericResponse>(it[0].toString())) {
                                is GenericResponse.Success -> {
                                    Log.d(TAG, "onIceGatheringChange: Answer sent!")
                                }

                                is GenericResponse.Error -> {
                                    Log.d(TAG, "onIceGatheringChange: Failed to set answer: ${response.message}")
                                }
                            }
                        }
                    }
                }

                override fun onDataChannel(channel: DataChannel?) {
                    channel!!
                    Log.d(TAG, "onDataChannel() called with: channel = $channel")
                    Log.d(TAG, "onDataChannel: connected!")

                    channel.registerObserver(object : DataChannel.Observer {
                        override fun onBufferedAmountChange(p0: Long) {
                            Log.d(TAG, "onBufferedAmountChange() called with: p0 = $p0")
                        }

                        override fun onStateChange() {
                            // todo
                            when (channel.state()!!) {
                                DataChannel.State.CONNECTING -> {
                                    Log.d(TAG, "onStateChange: connecting")
                                }
                                DataChannel.State.OPEN -> {
                                    Log.d(TAG, "onStateChange: open")
                                }
                                DataChannel.State.CLOSING -> {
                                    Log.d(TAG, "onStateChange: closing")
                                }
                                DataChannel.State.CLOSED -> {
                                    Log.d(TAG, "onStateChange: closed")
                                }
                            }
                        }

                        override fun onMessage(message: DataChannel.Buffer) {
                            Log.d(TAG, "onMessage() called with: message = $message")
                            // todo
                        }
                    })
                }
            })!!

        // create the video source and add it to the connection
        screenVideoSource = factory.createVideoSource(true)
        screenVideoTrack = factory.createVideoTrack("screen", screenVideoSource)
        connection.addTrack(screenVideoTrack)

        // sets the remote description
        connection.setRemoteDescription(object : SetSdpObserver {
            override fun onSetSuccess() {
                // success!
                Log.d(TAG, "onSetSuccess() called")

                updateNotificationText("Connecting - Controller description set")

                // create an answer to the offer and send it out to the server
                connection.createAnswer(object : CreateSdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        // success! now we set this as our local sdp then wait for the ice gathering
                        // to complete and send the offer to the server
                        Log.d(TAG, "onCreateSuccess() called with: sdp = $sdp")

                        connection.setLocalDescription(object : SetSdpObserver {
                            override fun onSetSuccess() {
                                Log.d(TAG, "onSetSuccess: local sdp set")
                            }

                            override fun onSetFailure(p0: String?) {
                                Log.e(TAG, "onSetFailure: failed to set local sdp: $p0")
                            }
                        }, sdp)
                    }

                    override fun onCreateFailure(message: String) {
                        // failed to create an answer
                        Log.d(TAG, "onCreateFailure() called with: message = $message")

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                applicationContext,
                                "Failed to create an SDP answer: $message",
                                Toast.LENGTH_LONG
                            ).show()

                            stopForeground(true)
                        }
                    }
                }, MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
                })
            }

            override fun onSetFailure(message: String) {
                // failed to set the remote sdp
                Log.d(TAG, "onSetFailure() called with: message = $message")

                Toast.makeText(
                    applicationContext,
                    "Failed to set the remote SDP: $message",
                    Toast.LENGTH_LONG
                ).show()

                stopForeground(true)
            }
        }, SessionDescription(SessionDescription.Type.OFFER, offer))

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Updates the notification text
     */
    private fun updateNotificationText(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(
            NOTIFICATION_ID,
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Slice Remote Control")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
        )
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "SliceRemoteControlServiceChannel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}