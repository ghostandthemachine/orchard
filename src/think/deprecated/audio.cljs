(ns think.audio
  (:use-macros [dommy.macros :only (sel sel1)])
  (:require [think.util :as util]))

(defn audio-context
  []
  (try
    (js/webkitAudioContext.)
    (catch js/Error e
      (js/alert
        "Web Audio API is not supported in this browser!"))))


(defn process-buffers
  [bufs context]
  (let [source         (.createBufferSource  context)
        frequency-box  (js/SpectrumBox. 2048 30 "fftbox" context)
        frequency-node (.getAudioNode frequency-box)
        wave-box       (js/SpectrumBox. 2048 1000 "wavebox" context)
        wave-node      (.getAudioNode wave-box)
        ]
    (.setValidPoints frequency-box 500)
    (set! (.-fillStyle (.getCanvasContext frequency-box))
      "rgb(150, 150, 150)")

    (set! (.-buffer source) (get bufs 0))
    (doto source
      (.connect frequency-node)
      (.connect (.-destination context))
      (.noteOn 0))
      (do
        (.connect frequency-node wave-node)
        (.connect wave-node (.-destination context))
        (.enable wave-box)
        (.enable frequency-box))))


(defn play-sound [& urls]
  (let [context       (audio-context)
        buffer-loader (js/BufferLoader.
                        context (clj->js urls)
                        #(process-buffers % context))]
    (.load buffer-loader)))


(defn setup-play-button
  [id]
  (.addEventListener (sel1 id) "click"
    (fn []
      (play-sound "flute.wav"))))


(defn setup-audio-drop
  [id]
  (util/file-drop id #(play-sound %)))

