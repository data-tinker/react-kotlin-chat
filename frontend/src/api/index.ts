const socket = new WebSocket("ws://localhost:8080/ws")

const connect = (cb: (msg: string) => void) => {
  console.log("Attempting Connection...")

  socket.onopen = () => {
    console.log("Successfully connected")
  }

  socket.onmessage = (msg) => {
    cb(msg.data)
  }

  socket.onclose = (event) => {
    console.log("Socket Closed Connection: ", event)
  }

  socket.onerror = (error) => {
    console.log("Socket Error: ", error)
  }
}

const sendMsg = (msg: string) => {
  socket.send(msg)
}

export { connect, sendMsg }
