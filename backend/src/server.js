const io = require('socket.io')(process.env.PORT || 8080);
require('dotenv').config()

const supabase = require("@supabase/supabase-js").createClient(process.env.SUPABASE_URL, process.env.SUPABASE_API_KEY)

io.on('connection', socket => {
  console.log('a socket has just connected');
  socket.on('create session', async (_) => {
    console.log('create a new session');

    const session_id = generate_id(5, true); // true: only generate numbers
    const controller_token = generate_id(32, false); // false: generate characters and numbers
    const remote_token = generate_id(32, false);

    // put this socket to a room identified with its session id and its token
    socket.join(`${session_id}-${remote_token}`)

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


function generate_id(length, only_numbers) {
    let result = '';
    const characters = only_numbers ? '1234567890' : 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * characters.length));
    }

   return result;
}
