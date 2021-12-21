package com.iyxan23.slice.shared

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * An interface class implements "create" functions of the SdpObserver so that only the "set"
 * functions will be mandatory to be implemented
 *
 * This class is used to make less clutter in the code
 */
interface SetSdpObserver : SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onCreateFailure(p0: String?) {}
}