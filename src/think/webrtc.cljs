(ns think.webrtc
  (:use-macros [dommy.macros :only [sel]])
  (:require [dommy.core :as dom]
            [dommy.template :as dt]
            [think.log :refer [log load-js]]))

; (def client-id* (atom 0))

; (defn gen-client-id
;   []
;   (swap! client-id* inc))

; (def peer-connections* (atom []))

; (defn create-rtc-peer-connection
;   []
;   )


(defn create-object-url
  [stream]
  (.createObjectURL js/webkitURL stream))

(defn view
  []
  [:div.container
    [:div.content
      [:div.video-box
        [:video#vid {:autoPlay true
                     :style {:border "2px solid black"
                             :width "480px"
                             :height "360px"}}]
        [:button.btn {:id "start-btn"} "Start"]]]])

(def stream* (atom nil))


(defn start-video
  [btn e]
  (log "start video")
  (let [video (first (sel :#vid))]
    (set! (.-disabled btn) true)
    (.webkitGetUserMedia js/navigator
      (clj->js {:video true})
      (fn [stream]
        (let [stream-url (create-object-url stream)]
          (reset! stream* stream)
          (log stream-url)
          (set! (.-src video)
            stream-url)))
      #())))

; (defn format-trace-msg
;   [text]
;   (str (/ (.now js/performance) 1000) ": " text))

; (defn trace
;   [text]
;   (if (= (last text) \n)
;     (let [msg (apply str (butlast text))]
;       (.log js/console (format-trace-msg msg)))
;     (.log js/console (format-trace-msg text))))


; (def local-stream* (atom nil))

; (defn got-stream
;   [stream]
;   (trace "Recieved local steam")
;   (js/attachMedaStream vid1 stream)
;   )

; (defn hangup
;   [pc1 pc2])




; function hangup() {
;   trace("Ending call");
;   pc1.close();
;   pc2.close();
;   pc1 = null;
;   pc2 = null;
;   btn3.disabled = true;
;   btn2.disabled = false;
; }

; function gotRemoteStream(e){
;   vid2.src = webkitURL.createObjectURL(e.stream);
;   trace("Received remote stream");
; }

; function iceCallback1(event){
;   if (event.candidate) {
;     pc2.addIceCandidate(new RTCIceCandidate(event.candidate));
;     trace("Local ICE candidate: \n" + event.candidate.candidate);
;   }
; }

; function iceCallback2(event){
;   if (event.candidate) {
;     pc1.addIceCandidate(new RTCIceCandidate(event.candidate));
;     trace("Remote ICE candidate: \n " + event.candidate.candidate);
;   }
; }


; (defn got-remote-stream
;   [vid ev]
;   (set! (.-src (create-object-url (.-stream ev))))
;   (trace "Received remote stream"))

; (defn ice-callback
;   [pc ev]
;   (when (.-candidate ev)
;     (.addIceCandidate pc (new js/RTCIceCandidate (.-candidate ev)))
;     (trace (str "Remote ICE candidate: \n " (.-candidate (.-candidate ev))))))







(defn init-handler
  []
  (let [btn (first (sel :#start-btn))]
    (dom/listen! btn :click (partial start-video btn))))

(defn init
  []
  (init-handler))




