(function(window) {

console.log("Starting...")

var start = (new Date()).getTime();
var fs = require("fs");
var walk = require("walkdir");

var head = window.document.querySelector("head");
var body = window.document.querySelector("body");

process.on("uncaughtException", uncaughtError);
window.onerror = uncaughtError;

function uncaughtError(e) {
  if (console) {
    console.log("ERROR:" + e);
    console.log(e.stack);
  }
}

function load_script(path, isFile) {
    script= document.createElement('script');
    script.type= 'text/javascript';
    script.async = false;
    if(isFile) {
        path = "file://" + path;
    }
    script.src= path;
    body.appendChild(script);
    return script;
}

function load_css(path, isFile) {
    css = document.createElement('link');
    css.type= 'text/css';
    if(isFile) {
        path = "file://" + path;
    }
    css.rel = "stylesheet";
    css.href = path;
    head.appendChild(css);
    return css;
}

function ext(path) {
    var i = path.lastIndexOf(".");
    var ext = path.substring(i+1);
    return ext;
}

function hasExt(path, exts) {
    var e = ext(path);
    for(var i in exts) {
        if ( e == exts[i] ) return true;
    }
    return false;
}

function readParse(json) {
    try {
        var code = fs.readFileSync(json);
    } catch (e) {
        return null;
    }
    return JSON.parse(code);
}


try {

    var thinker_app = "js/thinker.js";
    var order       = JSON.parse(fs.readFileSync("public/deploy/order.json"));

    order.forEach(function(path) {
        if(hasExt(path, ["css"])) load_css(path, false);
        if(hasExt(path, ["js"]))  load_script(path, false);
    });

    /* ready to go - let's load thinker_app */
    var script = load_script(thinker_app, false);
    script.onload = function() {
        try {
            think.objects.app.init();
        } catch (e) {
            uncaughtError(e);
        }
    }

} catch (e) {
    uncaughtError(e);
}

})(window);

