(ns think.util.nw)

(def ^:private gui (js/require "nw.gui"))

(defn window
  "If `window-object` is not specifed, then return current window's Window object,
  otherwise return `window-object`s Window object."
  ([] (.Window.get gui))
  ([window-object] (.Window.get gui window-object)))


(defn show
  []
  (let [w (window)]
    (.show w)
    (.focus w)))


(defn- append-menuitems [menu items]
  (let [ctor (.-MenuItem gui)]
    (doseq [item-options items]
      (.append menu (new ctor (clj->js item-options))))))


(defn menu
  "Create a new Menu. Items is a vector of MenuItem options.
  Options can have following fields: `label`, `icon`, `tooltip`, `type`, `click`, `checked`, `enabled` and `submenu`.
  See [MenuItem documentation](https://github.com/rogerwang/node-webkit/wiki/MenuItem)."
  [items]
  (let [ctor (.-Menu gui)]
    (doto (new ctor)
      (append-menuitems items))))


(defn menubar!
  "Create and set main Window's main Menu. Items is a vector of MenuItem options.
  Options can have following fields: `label`, `icon`, `tooltip`, `type`, `click`, `checked`, `enabled` and `submenu`.
  See [MenuItem documentation](https://github.com/rogerwang/node-webkit/wiki/MenuItem)."
  [items]
  (let [ctor (.-Menu gui)
        menu (new ctor (js-obj "type" "menubar"))]
    (append-menuitems menu items)
    (set! (.-menu (window)) menu)))


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
