
const spawn = require('child_process').spawn;

// run as below
// node tester.js /path/to/chatpeer.jar
// 
// the program will then spawn using the below command
// java -jar chatpeer.jar [-p port] [-i port]
//

const peerCmd = (jarLocation, hostPort, connPort) =>  [`-jar`, `${jarLocation}`, `-p`, `${hostPort}`, `-i`, `${connPort}`];

class EpochLog {
  constructor() {
    this.epoch = 0;
    this.counter = 0;
    this.capture = false;
    this.logs = []; 
    this.current = [];
    this.beginCapture();
  }

  beginCapture() {
    if (this.capture) {
      this.endCapture();
    }
    this.capture = true;
    this.current = [];
    this.counter = 0;
  }

  event(msg) {
    this.current.push(msg);
  }

  endCapture() {
    ++this.epoch;
    this.logs.push(this.current.slice());
    this.current = [];
    this.capture = false;
  }

  dump() {
    var ret = [];
    this.logs.forEach(e => ret.push(...e));
    return ret;
  }
};

class PeerLogs {
  constructor(){
    this.stdin = new EpochLog();
    this.stderr = new EpochLog();
    this.stdout = new EpochLog();
    this.captured = [];
    this.begin();
  }

  begin() {
    this.stdin.beginCapture();
    this.stdout.beginCapture();
    this.stderr.beginCapture();
  }

  end() {
    let stdinLogs = this.stdin.current.slice();
    let stdoutLogs = this.stdout.current.slice();
    let stderrLogs = this.stderr.current.slice();

    this.captured = [stdoutLogs, stderrLogs, stdinLogs];

    this.stdin.endCapture();
    this.stdout.endCapture();
    this.stderr.endCapture();

    return this.captured;
  }
}

class Peer {
  constructor(location, hostPort, connPort, id){
    this.process = spawn('java', peerCmd(location, hostPort, connPort));
    this.logs = new PeerLogs();
    this.id = id;
    this.spawnListeners()
  }

  write(message) {
    this.logs.stdin.event(message);

    this.process.stdin.cork();
    this.process.stdin.write(message);
    this.process.stdin.uncork();
  }

  kill() {
    this.process.kill();
  }

  spawnListeners() {
    const prefix = `[PEER] ${this.id}`;
    this.process.stdin.setEncoding("utf8");
    this.process.stdout.setEncoding("utf8");
    this.process.stderr.setEncoding("utf8");

    this.process.on('error', (err) => {console.error(`${prefix} ERROR`, err);});
    this.process.on("spawn", () => console.log(`${prefix} spawned`));

    this.process.stdout.on("data", (data) => {this.logs.stdout.event(data)});
    this.process.stderr.on("data", (data) => {this.logs.stderr.event(data);});
  }
};

var peers = [];

const main = ()  => {
  let args = process.argv.slice(2);
  let basePort = 10000;
  let size = 3;

  if (args.length < 1) {
    console.log("node tester.js /path/to/chatpeer.jar \n")
    return;
  }

  let jarLocation = args[0];

  for (i=0; i < size; ++i) {
    let peer = new Peer(jarLocation, basePort + i * 1000, basePort + i * 1000 + 1, i);
    peer.logs.begin();
    peers.push(peer);

    // peer0 hosts
    if (i == 0) {
      setTimeout(() => {
        peer.write(`#createroom room\r\n`);
      }, 500 * i);
    }

    // peerX, X>0 connects to peer0
    if (i > 0)  {
      setTimeout(() => {
        peer.write(`#connect localhost:${basePort}\r\n`);
      }, 500 * i);

      setTimeout(() => {
        peer.write(`#join room\r\n`);
      }, 1000 * i);

      setTimeout(() => {
        peer.write(`Hello World\r\n`);
      }, 1500 * i);
    }
  }

  setTimeout(() =>  {
    peers.forEach(peer => console.log(peer.logs.end()));
    cleanup();
  }, size * 2000)
}

const cleanup = () => peers.forEach(peer => peer.kill())

// kill all spawned processes on signal
process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);
process.on('exit', cleanup);

process.on('uncaughtException', function(e) {
  console.error('uncaught exception -> terminate: ', e.stack);
    cleanup()
});

process.on('unhandledRejection', function(e) {
    console.log('unhandled rejection -> terminate: ', e.stack);
    cleanup()
});


main();
