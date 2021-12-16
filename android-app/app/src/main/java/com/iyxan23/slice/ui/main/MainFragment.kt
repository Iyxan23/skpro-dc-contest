package com.iyxan23.slice.ui.main

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.databinding.FragmentMainBinding
import com.iyxan23.slice.domain.service.RemoteControlService
import com.iyxan23.slice.domain.service.SliceGestureService
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding

const val TAG = "MainFragment"

/**
 * This MainFragment is the landing page of this app, this is where the user can create or connect
 * to sessions.
 */
class MainFragment : Fragment(R.layout.fragment_main) {
    private val binding by viewBinding(FragmentMainBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonRemote.setOnClickListener {
            val app = requireActivity().application as App

            // check if our accessibility service is running
            if (isServiceEnabled(requireContext())) {
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

                    // check if we've connected, if not then connect!
                    if (!app.socket.connected()) {
                        Log.d(TAG, "onViewCreated: Not connected, connecting")
                        app.socket.connect()
                        Log.d(TAG, "onViewCreated: Connected!")
                    }

                    // we're going to ask for the server to create a session
                    app.socket.emit("create session", emptyArray()) {
                        // todo
                    }

                    // we start the remote control service
                    requireActivity().startForegroundService(
                        Intent(requireActivity(), RemoteControlService::class.java).apply {
                            putExtra("media_projection_token", it.data!!.clone() as Intent)
                        }
                    )
                }.launch(mediaProjectionManager.createScreenCaptureIntent())

            } else {
                // ask the user to enable the accessibility service in the settings
                AlertDialog.Builder(requireContext())
                    .setTitle("Permission")
                    .setMessage(
                        "We will redirect you to the accessibility settings, what you will need to do" +
                        "is to enable \"Slice Gesture Service\" for us to be able to control your" +
                        "device on behalf of the controller. After that you can click the button again."
                    )
                    .setPositiveButton("Ok") { dialog, _ ->
                        // redirect the user to accessibility settings
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        }

        binding.buttonController.setOnClickListener {
            findNavController()
                .navigate(R.id.action_mainFragment2_to_controlFragment)
        }
    }

    /**
     * Checks if our `SliceGestureService` is enabled
     */
    private fun isServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        for (enabledService in enabledServices) {
            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo

            if (enabledServiceInfo.packageName.equals(context.packageName) &&
                enabledServiceInfo.name.equals(SliceGestureService::class.java.name))
                return true
        }

        return false
    }
}