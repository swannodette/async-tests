// http://jsperf.com/messagechannel-vs-settimeout/2

// MessageChannel
// dispatch time: 466 message.js:30
// task time: 3102

// setTimeout

var task_count = 100000,
    cur_channel = 0;
    max_channels = 1000,
    channels = Array(max_channels);

function Channel() {
  this.tasks = [];
  this.c = new MessageChannel();

  var self = this;

  this.c.port1.onmessage = function(msg) {
    self.tasks.shift()();
  };
}

Channel.prototype.add_task = function(f) {
  this.tasks.push(f);
};

for(var i = 0; i < max_channels; i++) {
  channels[i] = new Channel();
}

var next_tick = function(f) {
  channels[cur_channel].add_task(f);
  channels[cur_channel].c.port2.postMessage(0);
  cur_channel = (cur_channel + 1) % max_channels;
};

var counter = 0,
    s = new Date();

var inc = function() {
  counter++;
  if(counter == task_count) {
    console.log("task time:", (new Date()-s));
  }
};

for(var i = 0; i < task_count; i++) {
  next_tick(inc, 0);
}

console.log("channel dispatch time:", (new Date())-s);
