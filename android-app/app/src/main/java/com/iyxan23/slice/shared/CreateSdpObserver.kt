package com.iyxan23.slice.shared

import org.webrtc.SdpObserver

/**
 * An interface class implements "set" functions of the SdpObserver so that only the "create"
 * functions will be mandatory to be implemented
 *
 * This class is used to make less clutter in the code
 */
interface CreateSdpObserver : SdpObserver {
    override fun onSetSuccess() {}
    override fun onSetFailure(p0: String?) {}
}