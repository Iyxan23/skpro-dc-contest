const app = require('express')();
const server = require('http').createServer(app);

const { Server } = require("socket.io");
const io = new Server(server);

io.on('connection', (req, res) => {
  // todo
});

let port = process.env.PORT
server.listen(port, () => {
  console.log(`listening on *:${port}`);
});
