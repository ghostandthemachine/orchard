(ns think.util
  (:use-macros [dommy.macros :only (sel sel1)])
  (:use [think.log :only (log log-obj)])
  (:require [dommy.core :as dom]))


(def ^:private gui     (js/require "nw.gui"))
(def ^:private fs      (js/require "fs"))
(def ^:private url     (js/require "url"))


(defn clj->json
  "Returns a JSON string from ClojureScript data."
  [data]
  (str (JSON/stringify (clj->js data)) "\n"))


(defn json->clj
  "Returns ClojureScript data for the given JSON string."
  [line]
  (js->clj (JSON/parse line)))


(defn parse-url
  "Returns a map by parsing a URL string."
  [url-str]
  (let [raw (js->clj (.parse url url-str))]
    {:protocol (.substr (get raw "protocol")
                        0 (dec (.length (get raw "protocol"))))
     :host (get raw "hostname")
     :port (js/parseInt (get raw "port"))
     :path (get raw "pathname")}))


(defn set-interval
  "Invoke the given function after and every delay milliseconds."
  [delay f]
  (js/setInterval f delay))


(defn clear-interval
  "Cancel the periodic invokation specified by the given interval id."
  [interval-id]
  (js/clearInterval interval-id))


(defn env
  "Returns the value of the environment variable k,
   or raises if k is missing from the environment."
  [k]
  (let [e (js->clj (.env node/process))]
    (or (get e k) (throw (str "missing key " k)))))


(defn trap
  "Trap the Unix signal sig with the given function."
  [sig f]
  (.on node/process (str "SIG" sig) f))


(defn exit
  "Exit with the given status."
  [status]
  (.exit node/process status))


;(def DATE-FORMATS
;  (let [f goog.i18n.DateTimeFormat.Format]
;    {:full-date       (.-FULL_DATE f)
;     :full-datetime   (.-FULL_DATETIME f)
;     :full-time       (.-FULL_TIME f)
;     :long-date       (.-LONG_DATE f)
;     :long-datetime   (.-LONG_DATETIME f)
;     :long-time       (.-LONG_TIME f)
;     :medium-date     (.-MEDIUM_DATE f)
;     :medium-datetime (.-MEDIUM_DATETIME f)
;     :medium-time     (.-MEDIUM_TIME f)
;     :short-date      (.-SHORT_DATE f)
;     :short-datetime  (.-SHORT_DATETIME f)
;     :short-time      (.-SHORT_TIME f)}))
;
;
;(defn format-date
;  "Returns a date using either a named format or a custom
;  formatting string like \"dd MMMM yyyy\"."
;  [date fmt]
;  (.format (goog.i18n.DateTimeFormat.
;             (or (get DATE-FORMATS fmt) fmt))
;           (js/Date. date)))
;
;
;(defn date-str
;  []
;  (format-date (js/Date.) :long-date))
;
;
;(defn date-json
;  []
;  (clj->json (date-str)))


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


(defn itemized-seq
  ([l] (itemized-seq l 0))
  ([l n]
   (when (< n (.-length l))
     (lazy-seq
       (cons (.item l n)
         (itemized-seq l (inc n)))))))

;(extend-type js/FileList
;   ISeqable
;   (-seq [this] (itemized-seq this))
;
;   ICounted
;   (-count [this] (.-length this))
;
;   IIndexed
;   (-nth [this n]
;        (.item this n))
;   (-nth [this n not-found]
;            (or (.item this n) not-found)))
;

;(extend-type js/HTMLCollection
;   ISeqable
;   (-seq [this] (itemized-seq this))
;
;   ICounted
;   (-count [this] (.-length this))
;
;   IIndexed
;   (-nth [this n]
;        (.item this n))
;   (-nth [this n not-found]
;            (or (.item this n) not-found)))
;

;(extend-type js/NodeList
;  ISeqable
;  (-seq [this] (itemized-seq this))
;
;  ICounted
;  (-count [this] (.-length this))
;
;  IIndexed
;  (-nth [this n]
;    (.item this n))
;  (-nth [this n not-found]
;        (or (.item this n) not-found)))

(defn foo
  ([] (foo 1))
  ([n] (log n)))

;(defn itemized-seq
;  ([l] (itemized-seq l 0))
;  ([l n] (log "foo")))
;(when (< n (.-length l))
;            (lazy-seq
;             (cons (.item l n)
;                (itemized-seq l (inc n)))))))
;
;(extend-type js/FileList
;  ISeqable
;  (-seq [this] (itemized-seq this))
;
;  ICounted
;  (-count [this] (.-length this))
;
;  IIndexed
;  (-nth [this n]
;    (.item this n))
;  (-nth [this n not-found]
;        (or (.item this n) not-found)))
;
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
;(defn write-file
;  "Write a string to a text file."
;  [path string]
;  (.writeFile fs path string))



;(defn on-ready [func]
;  (on js/document :DOMContentLoaded func))
