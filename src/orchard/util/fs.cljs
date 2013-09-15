(ns orchard.util.fs
  (:require-macros
    [cljs.core.async.macros :refer [go alt! alts!]])
  (:require
    [cljs.core.async    :refer (chan >! <! timeout)]))


(def ^:private fs   (js/require "fs"))
(def ^:private path (js/require "path"))


(defn exists?
  "Returns a channel that will receive true or false depending on whether a file
  exists at path."
  [path]
  (let [res-chan (chan)]
    (.exists fs path
      (fn [exists]
        (go (>! res-chan exists))))
    res-chan))


(defn join
  "Join path segments."
  [& args]
  (apply (.-join path) args))


(defn mkdir
  "Make a directory."
  [path]
  (.mkdirSync fs path))

