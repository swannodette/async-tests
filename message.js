// http://jsperf.com/messagechannel-vs-settimeout/2

// MessageChannel
// dispatch time: 466 message.js:30
// task time: 3102

// setTimeout

var task_count = 100000,
    taskq = [];
    c = new MessageChannel();

c.port1.onmessage = function(msg) {
  taskq.shift()();
};

var add_task = function(f) {
  taskq.push(f);
};

var next_tick = function(f) {
  add_task(f);
  c.port2.postMessage(0);
};

var counter = 0,
    s = new Date();

var inc = function() {
  counter++;
  if(counter == task_count) {
    document.getElementById("completion").innerHTML = "task time: " + (new Date()-s);
  }
};

for(var i = 0; i < task_count; i++) {
  next_tick(inc);
  //setTimeout(inc, 0);
}

document.getElementById("dispatch").innerHTML = "channel dispatch time: " + (new Date()-s);
