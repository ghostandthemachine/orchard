## Setup

To setup the native app do this:
   $ cp nw.icns /Applications/node-webkit/Contents/Resources/nw.icns
   $ mv /Applications/node-webkit /Applications/Thinker

* edit the /Applications/node-webkit/Contents/Info.plist and change the CFbundlename from node-webkit to Thinker.

-------------

* install node and npm (install link on node.js homepage)
* install node-cljs
    $ npm install -g clojure-script
* install supervisor
    $ npm install -g supervisor

## Running cljs on node

* run a cljs script directly
    $ ncljs server.cljs

* send to compiler server in background
    $ ncljs --server 5555
    ...
    $ ncljs --client 5555 server.cljs

* auto-compile on file save
    $ ncljs --server 5555
    $ supervisor -w hello.cljs -n exit -x ncljsc -- --client 5555 hello.cljs

* compile to disk
    $ ncljs --compile hello.cljs

* recompile and save on change
    $ supervisor -w hello.cljs -n exit -x ncljsc -- --client 4242 --compile hello.cljs

# Example project for node-webkit-cljs

1. `lein cljsbuild once`
2. [Download](https://github.com/rogerwang/node-webkit) latest node-webkit binary for your platform
3. See [How to run apps](https://github.com/rogerwang/node-webkit/wiki/How-to-run-apps) wiki page
   for platform-specific instructions.


## Native modules

To use modules with native library components (i.e. c or c++) we need to compile them specially for node-webkit.  The leveldown database used by pouchdb is one example, and this is how to get it working:

    $ cd node_modules/pouchdb/node_modules/levelup/node_modules/leveldown
    $ nw-gyp configure --target=0.4.2 -target_arch=ia32
    $ nw-gyp build

Note that the specific nw version needs to be specified to make it work.
