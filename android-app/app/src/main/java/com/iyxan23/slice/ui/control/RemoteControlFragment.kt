package com.iyxan23.slice.ui.control

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.domain.models.response.GenericResponse
import com.iyxan23.slice.shared.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.webrtc.*

/**
 * RemoteControlFragment is where the actual remote controlling happens
 */
class RemoteControlFragment : Fragment(R.layout.fragment_remote_control) {

    companion object { private const val TAG = "RemoteControlFragment" }

    private val socket by lazy { (requireActivity().application as App).socket }
    private lateinit var dataChannel: DataChannel

    private lateinit var connection: PeerConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // since we have connected to the session in the InsertSessionIDFragment, we now will just
        // do webrtc funsies

        // we need to generate ICE of this device so we can connect to the host (remote in slice)
        // but first we will need to create the peer connection
        val factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options().apply { disableEncryption = false })
            .createPeerConnectionFactory()

        connection = factory.createPeerConnection(iceServers, object : LogPeerConnectionObserver(TAG) {
            override fun onAddStream(stream: MediaStream?) {
                stream!!
                super.onAddStream(stream)
                TODO("instantiate the control surface and display this stream to it")
            }
        })!!

        // the data channel we will use to send gestures
        dataChannel = connection.createDataChannel("channel", DataChannel.Init())
        dataChannel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {
                Log.d(TAG, "onBufferedAmountChange() called with: amount = $amount")
            }

            override fun onStateChange() {
                Log.d(TAG, "onStateChange() called, new state: ${dataChannel.state()}")

                // check if this got opened
                when (dataChannel.state()!!) {
                    DataChannel.State.CONNECTING -> {

                    }

                    DataChannel.State.OPEN -> {

                    }

                    DataChannel.State.CLOSING -> {

                    }

                    DataChannel.State.CLOSED -> {

                    }
                }
            }

            override fun onMessage(message: DataChannel.Buffer) {
                Log.d(TAG, "onMessage() called with message = ${message.data}")
                TODO("when the other side sent a message to us")
            }
        })

        // and finally we will generate the ICE for this device
        connection.createOffer(object : CreateSdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                // success! set this as our local description and send that to the server!
                Log.d(TAG, "onCreateSuccess: offer made: \"${sdp.description}\", setting it as local sdp")

                connection.setLocalDescription(object : SetSdpObserver {
                    override fun onSetSuccess() {
                        Log.d(TAG, "onSetSuccess: local sdp successfully set! sending it to the server")

                        socket.emit(SOCKET_SET_ICE, arrayOf(sdp.description)) {
                            if (it[0] == null) {
                                Log.e(TAG, "onCreateSuccess: Invalid ack for set ice: ${it.toList()}")
                                return@emit
                            }

                            when (val response = Json.decodeFromString<GenericResponse>(it[0].toString())) {
                                GenericResponse.Success -> {
                                    Log.d(TAG, "onCreateSuccess: success")
                                }

                                is GenericResponse.Error -> {
                                    Log.d(TAG, "onCreateSuccess: error: ${response.message}")
                                }
                            }
                        }
                    }

                    override fun onSetFailure(p0: String?) {
                        Log.d(TAG, "onSetFailure: failed to set: $p0")

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                requireContext(),
                                "Failed to set offer as local SDP: $p0",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }, sdp)
            }

            override fun onCreateFailure(message: String) {
                // :(
                Log.e(TAG, "onCreateFailure: $message")
                Log.d(TAG, "onCreateFailure: state: ${connection.signalingState()}")

                Toast.makeText(
                    requireContext(),
                    "Failed to generate an offer: $message",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
        })

        // we will be listening for the other side's ice
        socket.utOnce(SOCKET_SET_ICE) {
            if (it[0] == null) {
                Log.e(TAG, "onCreate: Invalid SET ICE event arg: $it")

                Toast.makeText(
                    requireContext(),
                    "Server sent an invalid response",
                    Toast.LENGTH_LONG
                ).show()

                return@utOnce
            }

            Log.d(TAG, "onCreate: answer received: ${it[0].toString()}")

            // oke, it[0] is the ICE (answer), set that as a remote description!
            connection.setRemoteDescription(object : SetSdpObserver {
                override fun onSetSuccess() {
                    Log.d(TAG, "onSetSuccess: set remote description!")
                    Log.d(TAG, "onSetSuccess: signaling state: ${connection.signalingState()}")
                    Log.d(TAG, "onSetSuccess: connection state: ${connection.connectionState()}")
                    // success!! we have connected!
                }

                override fun onSetFailure(message: String) {
                    Log.d(TAG, "onSetFailure() called with: message = $message")

                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            requireContext(),
                            "Failed to set answer: $message",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }, SessionDescription(SessionDescription.Type.ANSWER, it[0].toString()))
        }
    }

    override fun onStop() {
        super.onStop()

        socket.off(SOCKET_SET_ICE)
    }
}