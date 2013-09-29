## Setup

To setup the native app do this:
   $ mv /Applications/node-webkit /Applications/Thinker
   $ cp nw.icns /Applications/Thinker/Contents/Resources/nw.icns
   $ ln -s /Users/rosejn/projects/thinker /Applications/Thinker/Contents/Resources/app.nw

* edit the /Applications/Thinker/Contents/Info.plist and change the CFbundlename from node-webkit to Thinker.

* The icon.png file should be a 1024x1024 image.

## Native modules

To use modules with native library components (i.e. c or c++) we need to compile them specially for node-webkit.  The leveldown database used by pouchdb is one example, and this is how to get it working:

    $ cd node_modules/pouchdb/node_modules/levelup/node_modules/leveldown
    $ nw-gyp configure --target=0.4.2 -target_arch=ia32
    $ nw-gyp build

Note that the specific nw version needs to be specified to make it work.

### Installing nodegit

    ; download the lib and its dependencies into node_modules
    npm install

    ; build as a universal binary (32-bit and 64-bit) to work with node-webkit
    cd node_modules/nodegit/vendor/libgit2
    rm -fr build/*
    cd build
    cmake .. -DCMAKE_OSX_ARCHITECTURES="i386;x86_64"
    make -j4

    ; make the nodegit library with nw-gyp
    nw-gyp rebuild  --target=0.7.2

Now you should be good to go.

