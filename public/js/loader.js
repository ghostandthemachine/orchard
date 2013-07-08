var initialized = false;

(function(window) {

var css_files = [
    "css/codemirror.css",
    "bootstrap/css/bootstrap.min.css",
    "font-awesome/css/font-awesome.min.css",
    "http://code.jquery.com/ui/1.10.2/themes/smoothness/jquery-ui.css",
    "css/thinker.css"];

var js_files = [
    "js/markdown_parser.js",
    "http://d3js.org/d3.v3.min.js",
    "js/codemirror.js",
    "js/cm/clojure.js",
    "js/cm/markdown.js",
    "js/cm/gfm.js",

    "js/jquery-1.9.0.js",
    "js/jquery.sortable.min.js",
    "http://code.jquery.com/ui/1.10.2/jquery-ui.js",
    "js/throttle.js",

    // "js/pdf/compatibility.js",
    // "js/pdf/l10n.js",
    "js/pdf.js",
    // "js/pdf/debugger.js",
    // "js/pdf/viewer.js",

    "bootstrap/js/bootstrap.min.js",
    "js/thinker.js"]


function log_error(e) {
  if (console) {
    console.log("ERROR:" + e);
    console.log(e.stack);
  }
}

var body = window.document.querySelector("body");
var head = window.document.querySelector("head");


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

function initialize() {
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

        script.onload = function() {
            try {
                var gui = require("nw.gui");
                if (gui.App.argv.indexOf('-test') > -1) {
                    console.log("Running unit tests...")
                        var results = cemerick.cljs.test.run_all_tests();
                        console.log(results);
                } else {
                    console.log("Starting application...")
                        think.objects.app.init();
                }
            } catch (e) {
                log_error(e);
            }
        }

    } catch (e) {
        log_error(e);
    }
}

initialize()

})(window);
