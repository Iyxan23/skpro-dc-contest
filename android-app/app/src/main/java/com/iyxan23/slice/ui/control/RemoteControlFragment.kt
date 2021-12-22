package com.iyxan23.slice.ui.control

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.databinding.FragmentRemoteControlBinding
import com.iyxan23.slice.shared.*
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import org.webrtc.*

/**
 * RemoteControlFragment is where the actual remote controlling happens
 */
class RemoteControlFragment : Fragment(R.layout.fragment_remote_control) {

    companion object { private const val TAG = "RemoteControlFragment" }

    private val binding by viewBinding(FragmentRemoteControlBinding::bind)
    private val socket by lazy { (requireActivity().application as App).socket }

    private lateinit var connection: PeerConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // since we have connected to the session in the InsertSessionIDFragment, we now will just
        // do webrtc funsies

        // we need to generate ICE of this device so we can connect to the host (remote in slice)
        // but first we will need to create the peer connection
        connection = PeerConnectionFactory(PeerConnectionFactory.Options().apply {
            disableEncryption = false
        }).createPeerConnection(iceServers, MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
        }, object : LogPeerConnectionObserver(TAG) {
            override fun onAddStream(stream: MediaStream?) {
                super.onAddStream(stream)
                TODO("when the other side starts streaming their screen")
            }
        })

        // the data channel we will use to send gestures
        val dataChannel = connection.createDataChannel("channel", DataChannel.Init())
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
                // success! send that to the server!
                socket.emit(SOCKET_SET_ICE, arrayOf(sdp.description))
            }

            override fun onCreateFailure(message: String) {
                // :(
                Log.e(TAG, "onCreateFailure: $message")

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
        socket.on(SOCKET_SET_ICE) {
            if (it[0] == null) {
                Log.e(TAG, "onCreate: Invalid SET ICE event arg: $it")

                Toast.makeText(
                    requireContext(),
                    "Server sent an invalid response",
                    Toast.LENGTH_LONG
                ).show()

                return@on
            }

            // oke, it[0] is the ICE (answer), set that as a remote description!
            connection.setRemoteDescription(object : SetSdpObserver {
                override fun onSetSuccess() {
                    Log.d(TAG, "onSetSuccess: Connected!")
                    // success!! we have connected!
                }

                override fun onSetFailure(message: String) {
                    Log.d(TAG, "onSetFailure() called with: message = $message")

                    Toast.makeText(
                        requireContext(),
                        "Failed to set answer: $message",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }, SessionDescription(SessionDescription.Type.ANSWER, it[0].toString()))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}