package com.iyxan23.slice.ui.control

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.databinding.FragmentInsertSessionIdBinding
import com.iyxan23.slice.domain.models.response.GenericResponse
import com.iyxan23.slice.shared.SOCKET_CONNECTION_CONFIRMED
import com.iyxan23.slice.shared.SOCKET_CONNECT_SESSION
import com.iyxan23.slice.shared.utEmit
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * InsertSessionIDFragment is where the user will insert the session ID, and it will also dispatch
 * session connection to the server, but the actual controlling and webrtc fun thingies are done
 * in RemoteControlFragment.
 */
class InsertSessionIDFragment : Fragment(R.layout.fragment_insert_session_id) {
    companion object { private const val TAG = "InsertSessionIDFragment" }

    private val binding by viewBinding(FragmentInsertSessionIdBinding::bind)
    private val socket by lazy { (requireActivity().application as App).socket }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // when the user finished entering the session ID
        binding.pinEntryEditText.setOnPinEnteredListener { sessionId ->
            // reset the error text
            binding.errorText.visibility = View.GONE
            binding.pinEntryEditText.isEnabled = false

            // then we ask the server if this session id exists
            // this utEmit function is an extension function that runs the ack in the ui  thread
            socket.utEmit(SOCKET_CONNECT_SESSION, arrayOf(sessionId)) { ack ->
                if (ack[0] == null) {
                    Log.e(TAG, "onViewCreated: Server sent an invalid ack: $ack")

                    Toast.makeText(
                        requireContext(),
                        "Server sent an invalid response",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@utEmit
                }

                // parse the response as it is a JSON
                when (val response = Json.decodeFromString<GenericResponse>(ack[0].toString())) {
                    is GenericResponse.Success -> {
                        // success! we're going to wait for a confirmation from the remote
                        socket.once(SOCKET_CONNECTION_CONFIRMED) {
                            // confirmed! replace this current fragment to the remote control fragment
                            parentFragmentManager.beginTransaction()
                                .replace(
                                    R.id.fragment_control_root,
                                    RemoteControlFragment::class.java,
                                    Bundle()
                                )
                                .commit()
                        }

                        binding.connectionStatusText.text = "Waiting for confirmation"
                    }

                    is GenericResponse.Error -> {
                        // error
                        binding.errorText.text = response.message
                        binding.errorText.visibility = View.VISIBLE
                        binding.pinEntryEditText.isEnabled = true // re-enable the pin entry
                    }
                }
            }
        }
    }
}