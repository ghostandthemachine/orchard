(ns orchard.util.cljs
  (:refer-clojure :exclude [js->clj])
  (:require [clojure.string :as string]))

(set! *print-fn* #(.log js/console (string/trim %)))

(defn ->dottedkw [& args]
  (keyword (string/join "." (map name (filter identity args)))))

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
