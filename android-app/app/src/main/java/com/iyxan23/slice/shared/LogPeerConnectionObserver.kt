package com.iyxan23.slice.shared

import android.util.Log
import org.webrtc.*

/**
 * An abstract class that implements every functions of PeerConnection.Observer with logm
 *
 * This is used to make every functions here are optional to be implemented, making it less of a
 * clutter in the peer connection code
 */
abstract class LogPeerConnectionObserver(private val TAG: String) : PeerConnection.Observer {
    override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "onStandardizedIceConnectionChange() called with: newState = $newState")
    }

    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
        Log.d(TAG, "onConnectionChange() called with: newState = $newState")
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        Log.d(TAG, "onTrack() called with: transceiver = $transceiver")
    }

    override fun onSignalingChange(state: PeerConnection.SignalingState?) {
        Log.d(TAG, "onSignalingChange() called with: state = $state")
    }

    override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "onIceConnectionChange() called with: state = $state")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Log.d(TAG, "onIceConnectionReceivingChange() called with: receiving = $receiving")
    }

    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
        Log.d(TAG, "onIceGatheringChange() called with: state = $state")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d(TAG, "onIceCandidate() called with: candidate = $candidate")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
        Log.d(TAG, "onIceCandidatesRemoved() called with: candidates = $candidates")
    }

    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
        Log.d(TAG, "onAddTrack() called with: receiver = $receiver, streams = $streams")
    }

    override fun onAddStream(stream: MediaStream?) {
        Log.d(TAG, "onAddStream() called with: stream = $stream")
    }

    override fun onRemoveStream(stream: MediaStream?) {
        Log.d(TAG, "onRemoveStream() called with: stream = $stream")
    }

    override fun onDataChannel(channel: DataChannel?) {
        Log.d(TAG, "onDataChannel() called with: channel = $channel")
    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded() called")
    }
}