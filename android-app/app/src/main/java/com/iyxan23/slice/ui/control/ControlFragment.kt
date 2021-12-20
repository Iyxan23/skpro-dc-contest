package com.iyxan23.slice.ui.control

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.iyxan23.slice.R

/**
 * This ControlFragment is where the client (the controller) controls a remote device it is
 * connected to in the session.
 *
 * It is the parent of `InsertSessionIDFragment` and `RemoteControlFragment`
 */
class ControlFragment : Fragment(R.layout.fragment_control) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // kick starts the InsertSessionIDFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_control_root, InsertSessionIDFragment::class.java, Bundle())
            .commit()
    }
}