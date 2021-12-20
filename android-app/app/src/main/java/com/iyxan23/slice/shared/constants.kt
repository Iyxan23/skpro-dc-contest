package com.iyxan23.slice.shared

import org.webrtc.PeerConnection

const val SERVER_URL = "wss://skpro-contest-slice-backend.herokuapp.com"

// a local development server running on the host on 127.0.0.1:8080
// read more: https://developer.android.com/studio/run/emulator-networking#networkaddresses
const val DEBUG_URL = "wss://10.0.2.2:8080"

const val SOCKET_EVENT_CONTROLLER_CONNECT = "controller connect"
const val SOCKET_EMIT_CREATE_SESSION = "create session"
const val SOCKET_EVENT_CONNECT_SESSION = "connect session"

val iceServers = listOf(PeerConnection.IceServer("stun:stun3.l.google.com:19302"))