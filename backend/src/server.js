const io = require('socket.io')(process.env.PORT || 8080);
require('dotenv').config()

const supabase = require("@supabase/supabase-js").createClient(process.env.SUPABASE_URL, process.env.SUPABASE_API_KEY)

let sockets = {}

io.on('connection', socket => {
  console.log('a socket has just connected');
  socket.on('create session', async (_) => {
    console.log('create a new session');

    const session_id = generate_id(5);
    const controller_token = generate_id(32);
    const remote_token = generate_id(32);

    // add this socket to our sockets list according to its id
    sockets[remote_token] = socket

    const { err } = await supabase
      .from('sessions')
      .insert([{'id': session_id, 'status': 'WAITING', 'controller_token': controller_token, 'remote_token': remote_token}]);

    if (err) {
      console.err(`err while inserting a new session row: ${err}`);
      ack({'type': 'Error', 'message': err.toString()});
    } else {
      ack({'type': 'Success', 'token': remote_token, 'session_id': session_id});
    }
  });
});

// remove a socket from our list if it disconnected
io.on('disconnect', socket => {
  for (const id in sockets) {
    if (sockets[id] === socket) {
      delete sockets[id];
      // todo: also do something with the database
      break;
    }
  }
})

function generate_id(length) {
    let result = '';
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < characters.length; i++) {
      result += characters.charAt(Math.floor(Math.random() * characters.length));
    }

   return result;
}