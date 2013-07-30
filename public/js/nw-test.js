var http = require('http')
var util = require('sys')

function now() {
  return new Date().getTime();
}


function test(id) {

  var t = now();
  var req = http.request({hostname: "127.0.0.1", port: 5984, path: "/foo/" + id, method: "PUT", agent: false},
  function(res) {
    res.setEncoding('utf-8');
    console.log('RESPONSE: ' + res.statusCode);
    console.log(util.inspect(res.headers));
    res.on('data', function(v) { console.log('BODY:' + v + '\ninsert time: ' + (now() - t))});
  });

  req.write('{"_id" : "' + id + '", "biz": "boz"}');
  req.end();
}
test(Math.floor(Math.random()*1000000));


function jquery_test(id) {
  var t = now();
  $.ajax({
    url: 'http://127.0.0.1:5984/foo/' + id,
    type: 'PUT',
    data: '{"_id" : "' + id + '", "biz": "boz"}',
}).done(function(res) {
  console.log('RES: ' + res)
   console.log('jquery insert time: ' + (now() - t)); 
});
}

jquery_test(Math.floor(Math.random()*1000000));