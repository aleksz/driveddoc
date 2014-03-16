function waitFor(testFx, onReady, timeOutMillis) {
	console.log('Start waiting for Jasmine');
    var maxtimeOutMillis = timeOutMillis ? timeOutMillis : 3001, //< Default Max Timeout is 3s
        start = new Date().getTime(),
        condition = false,
        interval = setInterval(function() {
            if ( (new Date().getTime() - start < maxtimeOutMillis) && !condition ) {
                // If not time-out yet and condition not yet fulfilled
//            	console.log('Waiting');
                condition = (typeof(testFx) === "string" ? eval(testFx) : testFx()); //< defensive code
            } else {
                if(!condition) {
                    // If condition still not fulfilled (timeout but condition is 'false')
                    console.log("'waitFor()' timeout");
                    phantom.exit(1);
                } else {
                    // Condition fulfilled (timeout and/or condition is 'true')
//                    console.log("'waitFor()' finished in " + (new Date().getTime() - start) + "ms.");
                    typeof(onReady) === "string" ? eval(onReady) : onReady(); //< Do what it's supposed to do once the condition is fulfilled
                    clearInterval(interval); //< Stop this interval
                }
            }
        }, 100); //< repeat check every 100ms
};

var page = require('webpage').create(),
  system = require('system'),
  t, address;

if (system.args.length === 1) {
  console.log('Usage: run-jasmine.js <some URL>');
  phantom.exit();
}

page.onError = function (msg, trace) {
    console.log(msg);
    trace.forEach(function(item) {
        console.log('  ', item.file, ':', item.line);
    })
}

page.onConsoleMessage = function(msg) {
	console.log(msg);
};

page.onResourceRequested = function (request) {
//    console.log('Request ' + JSON.stringify(request, undefined, 4));
};

t = Date.now();
address = system.args[1];

page.open(address, function(status) {
	// Check for page load success
    if (status !== "success") {
        console.log("Unable to access network");
    } else {
        // Wait for 'signin-dropdown' to be visible
        waitFor(function() {
            // Check in the page if a specific element is now visible
            return page.evaluate(function() {
//            	var a = $('body').html();
//            	console.log(a);
                return document.body.querySelector('.results') != null;
            });
        }, function() {
//           console.log("Shutting down Phantom.");
           phantom.exit();
        });        
    }
});