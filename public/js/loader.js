var initialized = false;

(function(window) {

var css_files = [
    "css/codemirror.css",
    "bootstrap/css/bootstrap.min.css",
    "font-awesome/css/font-awesome.min.css",
    "css/jquery-ui.css",
    "css/thinker.css"
    ];

var js_files = [
    "js/jquery-1.9.0.js",
    // "js/jquery.sortable.min.js",
    // "js/throttle.js",

    "js/codemirror.js",
    "js/cm/clojure.js",
    "js/cm/markdown.js",
    "js/cm/gfm.js",

    "js/markdown_parser.js",

    "js/tinymce/jscripts/tiny_mce/tiny_mce.js",


    // "js/pdf/compatibility.js",
    // "js/pdf/l10n.js",
    "js/pdf.js",
    // "js/pdf/debugger.js",
    // "js/pdf/viewer.js",

    // "js/d3.v3.min.js",

    "bootstrap/js/bootstrap.min.js",
    "js/thinker.js"
    ];


function log_error(e) {
  if (console) {
    console.log("ERROR:" + e);
    console.log(e.stack);
  }
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
    console.log("loading css: " + path)
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
    var gui = require("nw.gui");
    var test_mode = (gui.App.argv.indexOf('-test') > -1) ? true : false;

    try {
        process.on("uncaughtException", log_error);
        window.onerror = log_error;

        console.log("Loading CSS files...");
        css_files.forEach(function(path) {
            load_css(path, false);
        });


        console.log("Loading Javascript files...");
        js_files.forEach(function(path) {
            if (test_mode && path == "js/thinker.js") {
                console.log("Running unit tests");
            }
            load_script(path, false);
        });

        script.onload = function() {
            try {
                if (test_mode) {
                    gui.App.quit();
                } else {
                    console.log("Starting application...");
                    think.objects.app.init();
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

initialize()

})(window);
