const { createServer } = require('http');
const { Server } = require('socket.io')

require('dotenv').config()

const httpServer = createServer();
const io = new Server(httpServer, { /* options */ });

const supabase =
    require("@supabase/supabase-js")
      .createClient(process.env.SUPABASE_URL, process.env.SUPABASE_API_KEY)

// Socket.IO fun stuff =================

// things you need to know:
// rooms[1] is used to store what session the socket is connected to

io.on('connection', socket => {
  console.log(`${socket.id} has just connected`);

  socket.on('disconnect', () => {
    console.log(`${socket.id} has just disconnected`)
  })

  // called when a remote wanted to create a new session
  socket.on('create session', async (ack) => {
    console.log('create a new session');

    const session_id = generate_id(5);

    const { error } = await supabase
      .from('sessions')
      .insert({'id': session_id, 'remote_socket_id': socket.id});

    if (error) {
      console.error(`err while inserting a new session row: ${error}`);

      ack({'type': 'error', 'message': error.toString()});

    } else {
      // put this socket to a room of its session id
      socket.join(session_id);

      ack({'type': 'success', 'session_id': session_id});
    }
  });

  // called when a controller wanted to connect to a session
  socket.on('connect session', async (session_id, ack) => {
    // check if the provided session id exists
    const { data: [data], error: err1 } = await supabase
      .from('sessions')
      .select('*')
      .eq('id', session_id)
    
    // check if the data doesn't exist (invalid session id)
    if (!data) {
      ack({'type': 'error', 'message': 'Invalid session ID'})
      return

    } else if (err1) {
      console.error(`err while searching for session id ${session_id}: ${err1}`)

      ack({'type': 'error', 'message': err1.toString()})
      return
    }

    // it exists, we update the row to include this socket's id & set the status to be confirming
    const { error: err2 } = await supabase
      .from('sessions')
      .update({'controller_socket_id': socket.id, 'status': 'CONFIRMING'})
      .eq('id', session_id)

    if (err2) {
      console.error(`err while updating controller_socket_id of ${session_id}: ${err2}`)
      ack({'type': 'error', 'message': err2.toString})
      return
    }

    // success!
    ack({'type': 'success'})

    // now we ask the remote if they confirm the remote control connection
    socket.to(data.remote_socket_id).emit('controller connect confirm')
  });

  // called when the remote confirms connection from the controller
  socket.on('confirm connection', async (ack) => {
    // since this can only be called by a remote, we're going to find the session
    // this remote is in
    const { data: [data], error: err1 } = await supabase
      .from('sessions')
      .select('*')
      .eq('remote_socket_id', socket.id)

    if (!data) {
      ack({'type': 'error', 'message': 'Has not joined any session'})
      return

    } else if (err1) {
      console.error(`err on confirm connection for ${socket.id}: ${err1}`)
      ack({'type': 'error', 'message': err1.toString()})
      return
    }

    // session found! update the session status to CONFIRMED
    const { error: err2 } = await supabase
      .from('sessions')
      .update({'status': 'CONFIRMED'})
      .eq('id', data.id)

    if (err2) {
      console.error(`err on confirming for ${socket.id}: ${err2}`)
      ack({'type': 'error', 'message': err2.toString()})
      return
    }

    // success!
    ack({'type': 'success'})

    // and put the controller socket to the room according to its session id
    io.sockets.sockets.get(data.controller_socket_id).join(data.id)

    // then tell the controller socket that the connection has been confirmed
    socket.to(data.controller_socket_id).emit('connection confirmed')
  })

  // when this socket sets its sdp
  socket.on('set sdp', async (sdp, ack) => {

    // find the session id from the room the socket has joined before
    const session_id = find_session_id_from_rooms(socket.rooms)

    if (session_id == null) {
      // they haven't connected to any session yet!
      ack({'type': 'error', 'message': 'Has not connected to any session'})
      return
    }

    // get the session data
    const { data: [data], error } = await supabase
      .from('sessions')
      .select('*')
      .eq('id', session_id)
    
    if (error) {
      console.error(`failed to set sdp for socket ${socket.id} on session id ${session_id}: ${error}`)
      ack({'type': 'error', 'message': error.toString()})
      return
    }

    // check what side are we, then send the sdp to the opposite side
    if (socket.id === data.remote_socket_id) {
      // this is the remote trying to send the sdp answer to controller
      // but first check if the other side has connected to the session yet
      if (!data.controller_socket_id) {
        // nope, they haven't, return an error
        ack({'type': 'error', 'message': 'Other peer hasn\'t connected to this session yet'})
        return
      }

      // cool, they're there, send them the answer!
      io.sockets.sockets.get(data.controller_socket_id).emit('set sdp', sdp)

    } else if (socket.id === data.controller_socket_id) {
      // this is the controller trying to send the sdp offer to the remote
      // then send it to them!
      io.sockets.sockets.get(data.remote_socket_id).emit('set sdp', sdp)

    } else {
      // ?? what socket is this, its neither the controller nor the remote but its
      // connected to the session!?!??
      // remove them from the room session id & return
      console.error(`who is this socket ${socket.id}, data: ${JSON.stringify(data)}`)

      socket.leave(session_id)
      ack({'type': 'error', 'message': 'WHO THE HECK ARE YOU'})
      return
    }

    // success!
    ack({'type': 'success'})
  })
});

function generate_id(length) {
    let result = '';
    const characters = '1234567890';
    for (let i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * characters.length));
    }

   return result;
}

function find_session_id_from_rooms(rooms) {
  for (const room of rooms) {
    if (room.length == 5 && room.match(/^\d+$/) != null) {
      return room
    }
  }

  return null
}

const PORT = process.env.PORT || 8080;

console.log(`Starting to listen to ${PORT}`);
httpServer.listen(PORT);