(ns orchard.util.nw
  (:require
    [orchard.util.log :refer (log log-obj)]))

(def ^:private gui (js/require "nw.gui"))

(defn window
  "If `window-object` is not specifed, then return current window's Window object,
  otherwise return `window-object`s Window object."
  ([] (.Window.get gui))
  ([window-object] (.Window.get gui window-object)))


(defn show
  "Show the application window and focus."
  []
  (let [w (window)]
    (.show w)
    (.focus w)))


(defn toggle-full-screen
  "Go in and out of full screen mode."
  []
  (.toggleFullScreen (window)))


(defn clipboard
  "Get the system clipboard object."
  []
  (.Clipboard.get gui))


; NOTE: only "text" mimetype supported in nw currently...
(defn get-clipboard
  "Get the contents of the system clipboard."
  []
  (.get (clipboard) "text"))


(defn set-clipboard
  "Set the contents of the system clipboard."
  [txt]
  (.set (clipboard) txt "text"))


(defn data-path
  "Get the OS specific directory path for storing application user data."
  []
  (str (.-App.dataPath gui)))


(defn menu-item
  "Create a menu item from a map.
  e.g. {:label \"foo\", :icon \"foo.png\", :tooltip \"tip...\", :click my-fn}
  Other properties are :checked, :enabled, and :type, where the type can
  be either normal or separator."
  [fields]
  (let [ctor (.-MenuItem gui)]
    (new ctor (clj->js fields))))


(defn menu
  "Create a menu containing a set of menu itmes, each of which should be
  a map that will be converted into a menu-item: {:label \"foo\"}."
  [items]
  (let [ctor  (.-Menu gui)
        m     (new ctor)
        items (map menu-item items)]
    (doseq [item items]
      (.append m item))
    m))


(defn menu-bar
  "Create a menu bar.  The menus should be a vector of [menu-label [items...]] pairs."
  [menus]
  (let [ctor  (.-Menu gui)
        bar   (new ctor (clj->js {:type "menubar"}))]
    (doseq [[label items] (partition 2 menus)]
      (let [sub-menu (menu items)
            item     (menu-item {:label label})]
        (set! (.-submenu item) sub-menu)
        (.append bar item)))
    bar))


(defn set-menu-bar!
  "Set the application menu bar."
  [bar]
  (set! (.-menu (window)) bar))


(defn argv
  "Get the command line arguments when starting the app."
  []
  (seq (.-App.argv gui)))


(defn quit
  "Quit current app. This method will not send close event
  to windows and app will just quit quietly."
  []
  (.App.quit gui))


;; ## Tray

(when-not js/global.app-tray
  (set! js/global.app-tray (atom nil)))

(def ^:private app-tray js/global.app-tray)


(defn tray!
  "Create a new Tray, options is a map contains initial settings for the Tray.
  `options` can have following fields: `title`, `tooltip`, `icon` and `menu`.
  See [Tray documentation](https://github.com/rogerwang/node-webkit/wiki/Tray)."
  [options]
  (when-let [tray @app-tray]
    (.remove tray))
  (let [tray-constructor (.-Tray gui)]
    (reset! app-tray (new tray-constructor (clj->js options)))))


(defn update-tray
  "Assigns new value to one of the following options: `title`, `tooltip`, `icon` and `menu`.
  See [Tray documentation](https://github.com/rogerwang/node-webkit/wiki/Tray)."
  [option value]
  (aset @app-tray (name option) value))


