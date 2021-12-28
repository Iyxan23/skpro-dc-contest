# Slice
A TeamViewer-like app for a contest in the Sketchware Pro Discord server

### Diagram
<img src="/diagram.png"/>

Additional notes: After both peers connected using WebRTC, the remote will create a new video track that streams their screen, and the controller will open up a data channel where it will be sending gesture or commands to the controller.
