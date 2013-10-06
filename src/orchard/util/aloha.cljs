(ns orchard.util.aloha)

(defn aloha
  [elem]
  (.aloha (js/$ elem)))


(defn $aloha
  [sel]
  (aloha sel))


(defn mahalo
  [elem]
  (.mahalo (js/$ elem)))


(defn $mahalo
  [sel]
  (mahalo sel))

