const io = require("socket.io")(process.env.PORT || 8080);

import { createClient } from '@supabase/supabase-js'
import { url, api_key } from './creds.json'

const supabase = createClient(url, api_key)

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