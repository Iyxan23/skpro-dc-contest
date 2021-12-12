package com.iyxan23.slice.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.iyxan23.slice.R
import com.iyxan23.slice.databinding.FragmentMainBinding
import com.iyxan23.slice.domain.input.EventInput
import com.zhuinden.fragmentviewbindingdelegatekt.viewBinding

const val TAG = "MainFragment"

/**
 * This MainFragment is the landing page of this app, this is where the user can create or connect
 * to sessions.
 */
class MainFragment : Fragment(R.layout.fragment_main) {
    private val binding by viewBinding(FragmentMainBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonDispatchGestures.setOnClickListener {
            // we're going to try to slide down the notification thing
            Thread {
                EventInput
                    .injectTouch(
                        EventInput.TouchAction.FINGER_DOWN,
                        500f, 0f, 1f
                    )

                for (i in 1..700) {
                    EventInput
                        .injectTouch(
                            EventInput.TouchAction.FINGER_MOVE,
                            500f, i.toFloat(), 1f
                        )
                }

                EventInput
                    .injectTouch(
                        EventInput.TouchAction.FINGER_UP,
                        500f, 700f, 1f
                    )
            }.start()
        }

        binding.buttonListenGestures.setOnClickListener {

        }
    }
}