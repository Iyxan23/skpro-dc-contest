const io = require('socket.io')(process.env.PORT || 8080);
require('dotenv').config()

const supabase = require("@supabase/supabase-js").createClient(process.env.SUPABASE_URL, process.env.SUPABASE_API_KEY)

let sockets = {}

io.on('connection', socket => {
  socket.on('create session', _ => {
    const { data, err } =  await supabase
      .from('sessions')
      .insert([/* todo */])
  });
});

function generate_id(length) {
    var result           = '';
    var characters       = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    var charactersLength = characters.length;
    for ( var i = 0; i < length; i++ ) {
      result += characters.charAt(Math.floor(Math.random() * 
 charactersLength));
   }
   return result;
}