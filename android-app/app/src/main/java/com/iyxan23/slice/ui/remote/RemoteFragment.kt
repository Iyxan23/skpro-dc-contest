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
import com.iyxan23.slice.domain.service.RemoteControlService
import com.iyxan23.slice.shared.SOCKET_EMIT_CREATE_SESSION
import com.iyxan23.slice.shared.SOCKET_EVENT_CONTROLLER_CONNECT
import com.iyxan23.slice.ui.main.TAG
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class RemoteFragment : Fragment(R.layout.fragment_remote) {
    private val binding by viewBinding(FragmentRemoteBinding::bind)
    private val socket by lazy { (requireActivity().application as App).socket }
    private val sharedPref by lazy { requireContext().getSharedPreferences("data", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check if we've connected, if not then connect!
        if (!socket.connected()) {
            Log.d(TAG, "onViewCreated: Not connected, connecting")
            socket.connect()
            Log.d(TAG, "onViewCreated: Connected!")
        }

        // called when a controller is connecting to us!
        socket.on(SOCKET_EVENT_CONTROLLER_CONNECT) {
            if (it[0] == null) {
                Toast.makeText(requireContext(), "Server sent an invalid controller connect", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "onCreate: invalid controller connect: $it")
                return@on
            }

            // show a confirmation if the user wanted to connect to this controller
            AlertDialog.Builder(requireContext())
                .setTitle("Connect")
                .setMessage("A controller wanted to connect to your device, are you sure?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // ok! check for media projection and start the remote control service
                    startRemoteControl(it[0].toString())
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .create().show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // we're going to ask for the server to create a session
        socket.emit(SOCKET_EMIT_CREATE_SESSION, emptyArray()) { ack ->
            if (ack[0] == null) {
                showError("Server sent an invalid response, is the server we're connecting to valid?")
                return@emit
            }

            when (val response = Json.decodeFromString<CreateSessionResponse>(ack.toString())) {
                is CreateSessionResponse.Success -> {
                    // success! show the session id to the user and store the token elsewhere
                    Handler(Looper.getMainLooper()).post {
                        binding.sessionId.text = response.sessionId
                        binding.copySessionId.isEnabled = true
                    }

                    // save the token to sharedpref because yes
                    sharedPref
                        .edit()
                        .putString("token", response.token)
                        .apply()
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

    override fun onDestroy() {
        super.onDestroy()

        // remove socket events that were previously defined in onCreate
        socket.off(SOCKET_EVENT_CONTROLLER_CONNECT)
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