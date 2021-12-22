package com.iyxan23.slice.ui.remote

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.databinding.FragmentRemoteBinding
import com.iyxan23.slice.domain.models.response.CreateSessionResponse
import com.iyxan23.slice.domain.models.response.GenericResponse
import com.iyxan23.slice.domain.service.RemoteControlService
import com.iyxan23.slice.shared.*
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class RemoteFragment : Fragment(R.layout.fragment_remote) {
    companion object { private const val TAG = "RemoteFragment" }

    private val binding by viewBinding(FragmentRemoteBinding::bind)
    private val socket by lazy { (requireActivity().application as App).socket }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // will get called when the other peer sent its ICE (offer) to us
        val setIceEvent: (Array<Any?>) -> Unit = setIceEvent@{
            // fixme: do i need to run this in the UI thread?
            if (it[0] == null) {
                Log.e(TAG, "onCreate: invalid event on set ice: ${it.toList()}")
                showError("Server sent an invalid set ice event")
                return@setIceEvent
            }

            // now we got our offer at it[0], remote control go go go go brrrr
            startRemoteControl(it[0].toString())
        }

        // called when a controller is trying to connect to us and the server is asking for a
        // confirmation
        socket.utOnce(SOCKET_CONTROLLER_CONNECT_CONFIRM) {
            // show a confirmation if the user wanted to connect to this controller
            AlertDialog.Builder(requireContext())
                .setTitle("Confirmation")
                .setMessage("A controller is trying to connect to your device, confirm?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // good! we confirm the connection
                    socket.utEmit(SOCKET_CONFIRM_CONNECTION, emptyArray()) {
                        if (it[0] == null) {
                            Log.e(TAG, "onCreate: invalid response to confirm connection: ${it.toList()}")
                            showError("Server sent an invalid response")
                            return@utEmit
                        }

                        // check if this is successful
                        when (val response = Json.decodeFromString<GenericResponse>(it[0].toString())) {
                            is GenericResponse.Error -> {
                                Log.e(TAG, "onCreate: error on create session: ${response.message}", )
                                showError(response.message)
                            }

                            is GenericResponse.Success -> {
                                // nice! show that to the user
                                binding.connectionStatusText.text =
                                    "Connection confirmed, waiting for the other peer"

                                // and listen for the `set ice` event
                                socket.utOnce("set ice", setIceEvent)
                            }
                        }
                    }

                    // dismiss the dialog & show the connection status
                    dialog.dismiss()

                    binding.connectionStatus.visibility = View.VISIBLE
                    binding.connectionStatusText.text = "Confirming connection"
                }
                .setNegativeButton("No") { dialog, _ ->
                    // todo: implement cancelling confirmations
                    dialog.dismiss()
                }
                .create().show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // we're going to ask for the server to create a session
        socket.utEmit(SOCKET_CREATE_SESSION, emptyArray()) { ack ->
            if (ack[0] == null) {
                showError("Server sent an invalid response, is the server we're connecting to valid?")
                return@utEmit
            }

            when (val response = Json.decodeFromString<CreateSessionResponse>(ack[0].toString())) {
                is CreateSessionResponse.Success -> {
                    // success! show the session id to the user
                    binding.sessionId.text = response.sessionId
                    binding.copySessionId.isEnabled = true
                }

                is CreateSessionResponse.Error -> {
                    // :(
                    Handler(Looper.getMainLooper()).post { binding.sessionId.text = "Error" }
                    showError(response.message)
                }
            }
        }

        binding.copySessionId.setOnClickListener {
            val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("Slice session ID", binding.sessionId.text))

            Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage("Failed to create a new session: $message")
            .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                // go back to main fragment
                findNavController().popBackStack()
            }
            .create().show()
    }

    /**
     * Asks for media projection and kick-starts the remote control service
     */
    private fun startRemoteControl(controllerOffer: String) {
        // we're going to request for mediaprojection, then start the RemoteControlService
        // with the mediaprojection token passed through to it
        val mediaProjectionManager = requireActivity()
            .getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    requireContext(),
                    "We require the screen capture permission to be able to share your screen with the controller",
                    Toast.LENGTH_SHORT
                ).show()

                return@registerForActivityResult
            }

            // we start the remote control service
            requireActivity().startForegroundService(
                Intent(requireActivity(), RemoteControlService::class.java).apply {
                    putExtra("media_projection_token", it.data!!.clone() as Intent)
                    putExtra("controller_offer", controllerOffer)
                }
            )
        }.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}