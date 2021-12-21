package com.iyxan23.slice.shared

import org.webrtc.PeerConnection

const val SERVER_URL = "https://skpro-contest-slice-backend.herokuapp.com"

// a local development server running on the host on 127.0.0.1:8080
// read more: https://developer.android.com/studio/run/emulator-networking#networkaddresses
const val DEBUG_URL = "http://10.0.2.2:8080"

// socket.io events ==

// an event for the remote when a controller is connecting to the session ()
const val SOCKET_CONTROLLER_CONNECT = "controller connect"

// an emit event for the remote when it wants to create a session
const val SOCKET_CREATE_SESSION = "create session"

// an emit event for the controller when it wants to connect to a session created by a remote
const val SOCKET_CONNECT_SESSION = "connect session"

// an event and emit event for both remote and controller
// event: dispatched when the other side set their ICE
// emit: when we want to set our ice
const val SOCKET_SET_ICE = "set ice"

val iceServers = listOf(PeerConnection.IceServer("stun:stun3.l.google.com:19302"))