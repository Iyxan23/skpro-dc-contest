package com.iyxan23.slice.ui.control

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.databinding.FragmentInsertSessionIdBinding
import com.iyxan23.slice.domain.models.response.ConnectSessionResponse
import com.iyxan23.slice.shared.SOCKET_CONNECT_SESSION
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
            // then we ask the server if this session id exists
            socket.emit(SOCKET_CONNECT_SESSION, arrayOf(sessionId)) { ack ->
                if (ack[0] == null) {
                    Log.e(TAG, "onViewCreated: Server sent an invalid ack: $ack")

                    Toast.makeText(
                        requireContext(),
                        "Server sent an invalid response",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@emit
                }

                // parse the response as it is a JSON
                when (val response = Json.decodeFromString<ConnectSessionResponse>(ack[0].toString())) {
                    is ConnectSessionResponse.Success -> {
                        // success! replace the fragment to the next fragment to do webrtc fun
                        // stuff (and don't forget to pass the token)
                        parentFragmentManager.beginTransaction()
                            .replace(
                                R.id.fragment_control_root,
                                RemoteControlFragment::class.java,
                                Bundle().apply {
                                    putString("token", response.token)
                                }
                            )
                            .commit()
                    }

                    is ConnectSessionResponse.Error -> {
                        // error
                        binding.errorText.text = response.message
                        binding.errorText.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}