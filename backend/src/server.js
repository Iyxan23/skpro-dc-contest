const { createServer } = require('http');
const { Server } = require('socket.io')

require('dotenv').config()

const httpServer = createServer();
const io = new Server(httpServer, { /* options */ });

const supabase =
    require("@supabase/supabase-js")
      .createClient(process.env.SUPABASE_URL, process.env.SUPABASE_API_KEY)

// Socket.IO fun stuff =================

io.on('connection', socket => {
  console.log(`${socket.id} has just connected`);

  // called when a remote wanted to create a new session
  socket.on('create session', async (ack) => {
    console.log('create a new session');

    const session_id = generate_id(5);

    const { error } = await supabase
      .from('sessions')
      .insert([{'id': session_id, 'remote_socket_id': socket.id}]);

    if (error) {
      console.error(`err while inserting a new session row: ${error}`);

      ack({'type': 'error', 'message': error.toString()});
    } else {
      ack({'type': 'success', 'token': remote_token, 'session_id': session_id});
    }
  });

  // called when a controller wanted to connect to a session
  socket.on('connect session', async (session_id, ack) => {
    // check if the provided session id exists
    const { data, error } = await supabase
      .from('sessions')
      .select('*')
      .eq('id', session_id)
    
    // check if the data doesn't exist (invalid session id)
    if (data === undefined) {
      ack({'type': 'error', 'message': 'Invalid session ID'})
      return

    } else if (error) {
      console.error(`err while searching for session id ${session_id}: ${error}`)

      ack({'type': 'error', 'message': error.toString()})
      return
    }

    // it exists, we update the row to include this socket's id
    const { error } = await supabase
      .from('sessions')
      .update({'controller_socket_id': socket.id})
      .match({'id': session_id})

    if (error) {
      console.error(`err while updating controller_socket_id of ${session_id}: ${error}`)
      ack({'type': 'error', 'message': error.toString})
    } else {
      ack({'type': 'success'})
    }
  });
});

io.on('disconnect', socket => {
  console.log(`${socket.id} just disconnected`);
});

function generate_id(length) {
    let result = '';
    const characters = '1234567890';
    for (let i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * characters.length));
    }

   return result;
}

const PORT = process.env.PORT || 8080;

console.log(`Starting to listen to ${PORT}`);
httpServer.listen(PORT);