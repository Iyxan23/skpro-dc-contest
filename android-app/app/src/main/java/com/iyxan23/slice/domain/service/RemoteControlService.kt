package com.iyxan23.slice.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.iyxan23.slice.App
import com.iyxan23.slice.shared.iceServers
import org.webrtc.*

class RemoteControlService : Service() {
    companion object {
        private const val CHANNEL_ID = "SliceRemoteControlServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var connection: PeerConnection
    private val socket by lazy { (application as App).socket }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        socket // to retrieve the socket

        val offer = intent.getStringExtra("controller_offer")
        val mediaProjectionToken = intent.getParcelableExtra<Intent>("media_projection_token")

        createNotificationChannel()

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Slice Remote Control")
            .setContentText("Connecting")
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // initialize WebRTC things
        // (warning: there are an abundance of existential crisis comments, please watch out)

        // my gosh google's webrtc lib is so trash, i can't even find any documentation online
        // NOT A SINGLE BIT, except for the source code
        connection = PeerConnectionFactory(PeerConnectionFactory.Options().apply {
            disableEncryption = false
        }).createPeerConnection(iceServers, MediaConstraints().apply {
            // WTF IS THIS GOOGLE??!?!? WHY DO I NEED TO HARDCODE THE STRING AND WHY DON'T YOU JUST
            // CREATE AN ENUM TO LIST THEM ALL
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
        }, object : PeerConnection.Observer {
            // AND THIS!!??!?!?! WHY DO YOU NEED TO MAKE THEM ALL MANDATORY!?!? IT JUST ADDS A LOT
            // OF STUPID UNNEEDED HANDLERS, WHY NOT JUST MAKE IT LIKE JAVASCRIPT????!? `onIceCandidate`!?!??!
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "onSignalingChange() called with: p0 = $state")
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "onIceConnectionChange() called with: state = $state")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange() called with: receiving = $receiving")
            }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "onIceGatheringChange() called with: state = $state")
            }
            override fun onIceCandidate(candidate: IceCandidate?) {
                Log.d(TAG, "onIceCandidate() called with: candidate = $candidate")
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "onIceCandidatesRemoved() called with: candidates = $candidates")
            }
            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "onAddStream() called with: stream = $stream")
            }
            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "onRemoveStream() called with: stream = $stream")
            }
            override fun onDataChannel(channel: DataChannel) {
                Log.d(TAG, "onDataChannel() called with: channel = $channel")

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
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded() called")
            }
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "onAddTrack() called with: receiver = $receiver, streams = $streams")
            }
        })!!

        // sets the remote description
        connection.setRemoteDescription(object : SdpObserver {
            // AGAIN!?!?!?? WHY NOT JUST SEPARATE FOR SETTING AND CREATING SDPS ON DIFFERENT INTERFACES!?!?!?!
            override fun onCreateSuccess(description: SessionDescription?) {}
            override fun onCreateFailure(message: String) {}

            override fun onSetSuccess() {
                // success!
                Log.d(TAG, "onSetSuccess() called")

                updateNotificationText("Connecting - Controller description set")

                // create an answer to the offer and send it out to the server
                connection.createAnswer(object : SdpObserver {
                    override fun onSetSuccess() {}
                    override fun onSetFailure(message: String) {}

                    override fun onCreateSuccess(sdp: SessionDescription) {
                        // success! send that out to the server
                        Log.d(TAG, "onCreateSuccess() called with: sdp = $sdp")

                        socket.emit("set ice", arrayOf(sdp.description)) {
                            updateNotificationText("Connecting - ICE sent, waiting for a connection from the controller")
                        }
                    }

                    override fun onCreateFailure(message: String) {
                        // failed to create an answer
                        Log.d(TAG, "onCreateFailure() called with: message = $message")

                        Toast.makeText(
                            applicationContext,
                            "Failed to create an SDP answer: $message",
                            Toast.LENGTH_LONG
                        ).show()

                        stopForeground(true)
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