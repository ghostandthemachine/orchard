(ns think.util.core
  (:refer-clojure :exclude [js->clj clj->js])
  (:use-macros [dommy.macros :only (sel sel1)])
  (:use [think.util.log :only (log log-obj)])
  (:require [redlobster.promise :as p]
            [clojure.browser.repl :as repl]
            [dommy.core :as dom]
            [goog.i18n.DateTimeFormat :as date-format]))


(def ^:private url     (js/require "url"))


(defn js->clj
  "Recursively transforms JavaScript arrays into ClojureScript
  vectors, and JavaScript objects into ClojureScript maps.  With
  option ':keywordize-keys true' will convert object fields from
  strings to keywords."
  [x & options]
  (let [{:keys [keywordize-keys force-obj]} options
        keyfn (if keywordize-keys keyword str)
        f (fn thisfn [x]
            (cond
              (seq? x) (doall (map thisfn x))
              (coll? x) (into (empty x) (map thisfn x))
              (goog.isArray x) (vec (map thisfn x))
              (or force-obj
                  (identical? (type x) js/Object)
                  (identical? (type x) js/global.Object)) (into {} (for [k (js-keys x)]
                                                                     [(keyfn k)
                                                                      (thisfn (aget x k))]))
              :else x))]
    (f x)))

(defn clj->js
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings."
  [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    (map? x) (.-strobj (reduce (fn [m [k v]]
               (assoc m (clj->js k) (clj->js v))) {} x))
    (coll? x) (apply array (map clj->js x))
    :else x))

(defn clj->json
  "Returns a JSON string from ClojureScript data."
  [data]
  (str (JSON/stringify (clj->js data)) "\n"))


(defn json->clj
  "Returns ClojureScript data for the given JSON string."
  [line]
  (js->clj (JSON/parse line)))



(aset js/global "defonce-instances" (clj->js {}))

; (log "set global defonce-instances = " (aget js/global "defonce-instances"))


(defn parse-url
  "Returns a map by parsing a URL string."
  [url-str]
  (let [raw (js->clj (.parse url url-str))]
    {:protocol (.substr (get raw "protocol")
                        0 (dec (.length (get raw "protocol"))))
     :host (get raw "hostname")
     :port (js/parseInt (get raw "port"))
     :path (get raw "pathname")}))

(def DATE-FORMATS
  (let [f date-format/Format]
    {:full-date       (.-FULL_DATE f)
     :full-datetime   (.-FULL_DATETIME f)
     :full-time       (.-FULL_TIME f)
     :long-date       (.-LONG_DATE f)
     :long-datetime   (.-LONG_DATETIME f)
     :long-time       (.-LONG_TIME f)
     :medium-date     (.-MEDIUM_DATE f)
     :medium-datetime (.-MEDIUM_DATETIME f)
     :medium-time     (.-MEDIUM_TIME f)
     :short-date      (.-SHORT_DATE f)
     :short-datetime  (.-SHORT_DATETIME f)
     :short-time      (.-SHORT_TIME f)}))


(defn format-date
  "Returns a date using either a named format or a custom
  formatting string like \"dd MMMM yyyy\"."
  [date fmt]
  (.format (goog.i18n.DateTimeFormat.
             (or (get DATE-FORMATS fmt) fmt))
           (js/Date. date)))


(defn date-str
  []
  (format-date (js/Date.) :long-date))


(defn date-json
  []
  (clj->json (js/Date.)))


(defn uuid
  "returns a type 4 random UUID: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx"
  []
  (let [r (repeatedly 30 (fn [] (.toString (rand-int 16) 16)))]
    (apply str (concat (take 8 r) ["-"]
                       (take 4 (drop 8 r)) ["-4"]
                       (take 3 (drop 12 r)) ["-"]
                       [(.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16)]
                       (take 3 (drop 15 r)) ["-"]
                       (take 12 (drop 18 r))))))


(defn clipboard []
  (.get js/Clipboard))


(defn read-clipboard []
  (.get (clipboard)))


(defn refresh-window []
  (js/window.location.reload true))


(defn promise-logger
  [prom]
  (p/on-realised prom log-obj log-obj))


(defn itemized-seq
  ([l] (itemized-seq l 0))
  ([l n]
   (when (< n (.-length l))
     (lazy-seq
       (cons (.item l n)
         (itemized-seq l (inc n)))))))


(defn file-drop
  [elem handler]
  (set! (.-ondragover js/window) (fn [e] (.preventDefault e) false))
  (set! (.-ondrop js/window) (fn [e] (.preventDefault e) false))

  (let [elem (sel1 elem)]
    (set! (.-ondragover elem)
          (fn []
            (this-as spot
                     (set! (.-className spot) "hover"))
            false))

    (set! (.-ondragend elem)
          (fn []
            (this-as spot
                     (set! (.-className spot) ""))))

    (set! (.-ondrop elem)
          (fn [e]
            (.preventDefault e)

            (let [fl (.-files (.-dataTransfer e))
                  file-list (map #(.item fl %) (range (.-length fl)))]
              (when handler
                (handler (doall file-list))))
            false))))


(defn handle-file-select [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (let [files (.-files (.-dataTransfer evt))]
    (dotimes [i (.-length files)]
      (let [rdr (js/FileReader.)
            the-file (aget files i)]
        (log "File path: " (.-path the-file))
        (comment set! (.-onload rdr)
              (fn [e]
                (let [file-content (.-result (.-target e))
                      file-name (if (= ";;; " (.substr file-content 0 4))
                                  (let [idx (.indexOf file-content "\n\n")]
                                    (.log js/console idx)
                                    (.slice file-content 4 idx))
                                  (.-name the-file))]
                  (.log js/console (str "file-name " file-name))
                  (.set storage file-name file-content)
                  (swap! list-of-code conj file-name))))
        (.readAsText rdr the-file)))))


(defn handle-drag-over
  [evt]
  (.stopPropagation evt)
  (.preventDefault evt)
  (set! (.-dropEffect (.-dataTransfer evt)) "copy"))


(defn setup-drop-zone
  [id]
  (let [dz (first (sel id))]
    (dom/listen! dz :dragover handle-drag-over)
    (dom/listen! dz :drop     handle-file-select)))



(defn open-window
  [url & {:as options}]
  (let [o (merge {:x 0 :y 0 :width 400 :height 600} options)
        opt-str (format "screenX=%d,screenY=%d,width=%d,height=%d"
                        (:x o) (:y o) (:width o) (:height o))]
    (.open js/window url nil opt-str)))


;
;(defn setup-tray [title]
;  "Creates a tray menu in upper-right app tray."
;  (nw/tray! {:title title
;             :menu (nw/menu [{:label "Show" :click #(.show (nw/window))}
;                             ;{:label "Close all" :click #(.App.closeAllWindows)}
;                             {:type "separator"}
;                             {:label "Editor..." :click editor-window}
;                             {:label "Quit" :click nw/quit}
;                             ])}))
;
;

(defn data
  ([elem attr]
    (.getAttribute elem attr))
  ([elem attr value]
    (.setAttribute elem attr value)))

; element.addEventListener('webkitAnimationEnd', function(){
;     this.style.webkitAnimationName = '';
; }, false);

; document.getElementById('button').onclick = function(){
;     element.style.webkitAnimationName = 'shake';
;     // you'll probably want to preventDefault here.
; };

; (defn create-style-html
;   [n styles]
;   (reduce
;     (fn [ihtml [k v]]
;       (str ithml (str (name k) ":" v ";")))
;     (str n " {")
;     (into [] styles)))


; (defn create-style-html
;   [n styles]
;   (clojure.string/join ";"
;     (reduce
;       (fn [ihtml [k v]]
;         (conj ithml (str (name k) ":" v ";")))
;       [(str n " {")]
;       (into [] styles))))


; (defn style
;   [n & opts]
;   (let [s (.createElement js/document "style")
;         inner-html
;                     ; (doseq [[k v] (partition 2 opts)]
;                     ;   (aset s (name k) v))
;     s))


; (defn add-animation
;   [elem animation-name]
;   (dom/set-style! elem {:webkitAnimationName animation-name}))

; (defn clear-animation
;   [elem]
;   (dom/set-style! elem {:webkitAnimationName ""}))

; (defn do-animation
;   [elem animation]
;   (dom/set-style! elem animation)
;   (dom/listen! elem :webkitAnimationEnd


(defn- itemized-seq
  ([l] (itemized-seq l 0))
  ([l n] (when (< n (.-length l))
            (lazy-seq
             (cons (.item l n)
                (itemized-seq l (inc n)))))))

(extend-type js/FileList
  ISeqable
  (-seq [this] (itemized-seq this))

  ICounted
  (-count [this] (.-length this))

  IIndexed
  (-nth [this n]
    (.item this n))
  (-nth [this n not-found]
        (or (.item this n) not-found)))

; (extend-type js/Array
;   ISeqable
;   (-seq [this] (itemized-seq this))

;   ICounted
;   (-count [this] (.-length this))

;   IIndexed
;   (-nth [this n]
;     (.item this n))
;   (-nth [this n not-found]
;         (or (.item this n) not-found)))

(defn refresh
  "Refresh the current page."
  []
  (js/window.location.reload true))


(defn clipboard
  "Get the js clipboard."
  []
  (.get js/Clipboard))


(defn read-clipboard
  "Get the current contents of the clipboard."
  []
  (.get (clipboard)))


(defn open-window
  [url & {:as options}]
  (let [o (merge {:x 0 :y 0 :width 400 :height 600} options)
        opt-str (format "screenX=%d,screenY=%d,width=%d,height=%d"
                        (:x o) (:y o) (:width o) (:height o))]
    (.open js/window url nil opt-str)))


(defn editor-window
  []
  (util/open-window "editor.html"))


(defn load-js
  [file-name]
  (dom/append! (first (sel :head))
    (dt/node [:script {:src file-name}])))


(defn start-repl-server
  []
  (repl/connect "http://127.0.0.1:9000/repl"))


(defn await
  "Takes a seq of promises and produces a promise that will resolve to a seq of
  their values."
  [promises]
  (let [await-all (= (first promises) :all)
        promises (if await-all (rest promises) promises)
        p (p/promise)
        total (count promises)
        count (atom 0)
        done (atom false)]
    (doseq [subp promises]
      (let [succ (fn [_]
                   (when (not @done)
                     (swap! count inc)
                     (when (= total @count)
                       (reset! done true)
                       (p/realise p (doall (map #(js->clj (deref %)) promises))))))
            fail (if await-all succ
                     (fn [err]
                       (when (not @done)
                         (reset! done true)
                         (p/realise-error p err))))]
        (p/on-realised subp succ fail)))
    p))


(defn bound-do
  [a* handler]
  (add-watch a* :mode-toggle-watch
    (fn [k elem* ov nv]
      (handler nv))))



(defn mixin
  [obj obj2]
  (let [mix js/mixin]
    (mix obj (clj->js obj2))))


;; Basic data structure helpers


(defn insert-at [coll pos item]
  "Insert an item at a specific position within a collection."
  (let [vec (into [] coll)]
    (apply merge (subvec vec 0 pos) item (subvec vec pos))))


(defn indexed
  "Returns a lazy sequence of [index, item] pairs, where items come
  from 's' and indexes count up from zero.
  (indexed '(a b c d))  =>  ([0 a] [1 b] [2 c] [3 d])"
  [s]
  (map vector (iterate inc 0) s))


(defn positions
  "Returns a lazy sequence containing the positions at which pred
   is true for items in coll."
  [pred coll]
  (for [[idx elt] (indexed coll) :when (pred elt)] idx))


(defn index-of
  "Get the index of a value within a collection."
  [v coll]
  (first (positions #{v} coll)))


(defn aget-in
  [obj ks]
  (reduce
    (fn [o k]
      (aget o (name k)))
    obj ks))


(defn aset-in
  [obj ks v]
  (aset obj (name (first ks))
    (clj->js
      (reduce
        (fn [m k]
          {k m})
        {(last ks) v}
        (butlast (rest ks))))))

(defn by-id
  ([id]
    (by-id js/document id))
  ([document id]
    (.getElementById document (name id))))