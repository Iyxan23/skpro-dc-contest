package com.iyxan23.slice.ui.main

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iyxan23.slice.App
import com.iyxan23.slice.R
import com.iyxan23.slice.databinding.FragmentMainBinding
import com.iyxan23.slice.domain.service.SliceGestureService
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding
import io.socket.client.Socket

const val TAG = "MainFragment"

/**
 * This MainFragment is the landing page of this app, this is where the user can create or connect
 * to sessions.
 */
class MainFragment : Fragment(R.layout.fragment_main) {
    private val binding by viewBinding(FragmentMainBinding::bind)
    private val socket by lazy { (requireActivity().application as App).socket }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // connect the socket if it hasn't been connected
        if (!socket.connected()) {
            Log.d(TAG, "onCreate: socket isn't connected, connecting now")
            socket.connect()
        }

        // and lift the loading screen when the socket has connected
        socket.once(Socket.EVENT_CONNECT) {
            Log.d(TAG, "onCreate: connected")
            // lift the loading screen
            binding.loadingOverlay.animate()
                .alpha(0f)
                .setDuration(300L)
                .withEndAction {
                    binding.loadingOverlay.visibility = View.GONE
                }
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.w(TAG, "onCreate: failed to connect to socket ${it.asList()}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonRemote.setOnClickListener {
            // check if our accessibility service is running
            if (isServiceEnabled(requireContext())) {
                findNavController()
                    .navigate(R.id.action_mainFragment2_to_remoteFragment)

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