const express = require('express')
const { createServer } = require('http');
const { Server } = require('socket.io')

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, { /* options */ });

require('dotenv').config()

const supabase =
    require("@supabase/supabase-js")
      .createClient(process.env.SUPABASE_URL, process.env.SUPABASE_API_KEY)

io.on('connection', socket => {
  console.log('a socket has just connected');

  // called when a remote wanted to create a new session
  socket.on('create session', async (_) => {
    console.log('create a new session');

    const session_id = generate_id(5, true); // true: only generate numbers
    const controller_token = generate_id(32, false); // false: generate characters and numbers
    const remote_token = generate_id(32, false);

    // put this socket to a room identified with its session id and its token
    socket.join(`${session_id}-${remote_token}`)

    const { error } = await supabase
      .from('sessions')
      .insert([{'id': session_id, 'status': 'WAITING', 'controller_token': controller_token, 'remote_token': remote_token}]);

    if (error) {
      console.error(`err while inserting a new session row: ${error}`);

      ack({'type': 'error', 'message': error.toString()});
    } else {
      ack({'type': 'success', 'token': remote_token, 'session_id': session_id});
    }
  });

  // called when a controller wanted to connect to a session
  socket.on('connect session', async (session_id) => {
    // check if the provided session id exists
    const { data, error } = await supabase
      .from('sessions')
      .select('*')
      .eq('id', session_id)
    
    // check if the data doesn't exist (invalid session id)
    if (data === undefined) {
      ack({'type': 'error', 'message': 'Invalid session ID'})
    } else if (error) {
      console.error(`err while searching for session id ${session_id}: ${error}`)

      ack({'type': 'error', 'message': error.toString()})
    } else {
      ack({'type': 'success', 'token': data.controller_token})
    }
  })
});

app.get('/', (req, res) => {
  console.log('hello world!');
  res.send('hello world!');
})

function generate_id(length, only_numbers) {
    let result = '';
    const characters = only_numbers ? '1234567890' : 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * characters.length));
    }

   return result;
}

const PORT = process.env.PORT || 8080;

console.log(`Starting to listen to ${PORT}`);
httpServer.listen(PORT);