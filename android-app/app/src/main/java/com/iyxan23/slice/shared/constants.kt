package com.iyxan23.slice.shared

import org.webrtc.PeerConnection

const val SERVER_URL = "https://skpro-contest-slice-backend.herokuapp.com"

// a local development server running on the host on 127.0.0.1:8080
// read more: https://developer.android.com/studio/run/emulator-networking#networkaddresses
const val DEBUG_URL = "http://10.0.2.2:8080"

// socket.io events ==
// if you're confused, take a look at the diagram in github

// REMOTE - create session -> SERVER
//
// an emit event for the remote when it wants to create a session
const val SOCKET_CREATE_SESSION = "create session"

// SERVER - controller connect confirm -> REMOTE
//
// an event for the remote when a controller is trying to connect to the session and the server is
// asking for a conformation for the remote if it wants to get connected to this specific controller
//
// the remote will then respond back by emitting SOCKET_CONFIRM_CONNECTION
const val SOCKET_CONTROLLER_CONNECT_CONFIRM = "controller connect confirm"

// REMOTE - confirm connection -> SERVER
//
// an event that gets emitted by the remote when it confirms a connection from a specific controller
// this is called after SOCKET_CONTROLLER_CONNECT_CONFIRM
const val SOCKET_CONFIRM_CONNECTION = "confirm connection"

// SERVER - connection confirmed -> CONTROLLER
//
// an event for the controller when its connection is confirmed by the remote, this is called by the
// server when the REMOTE emits SOCKET_CONFIRM_CONNECTION
const val SOCKET_CONNECTION_CONFIRMED = "connection confirmed"

// CONTROLLER - connect session -> SERVER
//
// an event that gets emitted by the controller when it wants to connect to a session created by a
// remote
const val SOCKET_CONNECT_SESSION = "connect session"

// an event and emit event for both remote and controller
// event: dispatched when the other side set their ICE
// emit: when we want to set our ice
const val SOCKET_SET_ICE = "set ice"

// STUN and TURN servers
val iceServers = listOf(
    PeerConnection.IceServer("stun:stun.l.google.com:19302"),
    PeerConnection.IceServer("stun:stun1.l.google.com:19302"),
    PeerConnection.IceServer("stun:stun2.l.google.com:19302"),
    PeerConnection.IceServer("stun:stun3.l.google.com:19302"),
    PeerConnection.IceServer("stun:stun4.l.google.com:19302"),
)