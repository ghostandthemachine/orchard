var node_require = require;

var initialized = false;

(function(window) {

var css_files = [
    "bootstrap/css/bootstrap.min.css",
    "font-awesome/css/font-awesome.min.css",
    "css/style.css",
    "css/slide-menu.css"];

var js_files = [
    "js/jquery-1.9.0.js",
    "bootstrap/js/bootstrap.min.js"];


function log_error(err) {
  if (console) {
    console.log(err.stack);
  }

  throw err;
}

var body = window.document.querySelector("body");
var head = window.document.querySelector("head");


function load_script(path, isFile) {
    console.log("loading script: " + path)
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
    //console.log("loading css: " + path)
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


function initialize() {
    var gui = node_require("nw.gui");
    var test_mode = (gui.App.argv.indexOf('-test') > -1) ? true : false;
    var app_script;

    try {



        process.on("uncaughtException", log_error);
        window.onerror = log_error;

        console.log("Loading CSS files...");
        css_files.forEach(function(path) {
            load_css(path, false);
        });

        console.log("Loading Javascript files...");
        js_files.forEach(function(path) {
            load_script(path, false);
        });

        if (test_mode) {
            console.log("Running tests...")
            load_script("js/tests.js", false);
        } else {
            console.log("Loading app...");

            app_script = load_script("js/app.js", false);
        }

        app_script.onload = function() {
            try {
                if (test_mode) {
                    setTimeout(gui.App.quit, 5000);
                } else {
                    console.log("Starting app...");
                    orchard.core.init();
                }
            } catch (e) {
                log_error(e);
                if (test_mode) {
                    gui.App.quit();
                }
            }
        }

    } catch (e) {
        log_error(e);
        gui.App.quit();
    }
}

function aloha(sel) {
    $(sel).aloha()
}

console.log("Initializing application...");
initialize();

})(window);
