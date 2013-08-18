(ns think.observe
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [think.util.dom :refer (append $ by-id)]
            [cljs.core.async :refer [chan >! <! put! timeout close!]]
            [think.util.log :refer (log log-obj)]))


(defn js-style-name
  [attr-name]
  (let [start   (re-seq #"^[A-Za-z0-9]+" attr-name)
        end     (re-seq #"\-[A-Za-z][a-z0-9]*" attr-name)
        cleaned (map #(clojure.string/capitalize (apply str (rest %))) end)]
    (apply str (flatten (merge cleaned start)))))

(defn clj-style-name
  [attr-name]
  (let [start   (re-seq #"^[a-z0-9]+" attr-name)
        end     (re-seq #"[A-Z][a-z0-9]*" attr-name)
        cleaned (map #(str "-" (clojure.string/lower-case %)) end)]
    (apply str (flatten (merge cleaned start)))))

(def last-mutation* (atom nil))

(defn handle-mutations
  [mutations]
  (doseq [mutation mutations]
    (.log js/console (reset! last-mutation* mutation))))


(defn handle-summary
  [summaries]
  (doseq [summary summaries]
    (log-obj summary)))


(defn observer [handler]
  (new js/WebKitMutationObserver handler))


; (defn init-summary-observer
;   ([handler]
;     (summary-observer handler [{:all true}]))
;   ([handler queries]
;     (new js/MutationSummary (clj->js {:callback handler :queries queries}))))


(defn observe
  [node handler & config]
  (.observe (observer handler) node (clj->js (reduce #(assoc %1 (js-style-name (name %2)) true) {} config))))


(defn add-test-elem []
  (append js/document.body
    (crate.core/html
      [:div#test-elem])))


(def mutations* (atom {:mutations nil
                       :list []}))


(defn handle-node-ready
  [node chan mutations]
  (.log js/console mutations)
  (doseq [m mutations]
    (.log js/console m)
    ; (swap! mutations* #(assoc % :list (conj (:list %) m)
    ;                             :mutations mutations))
    )
  ; (let [addmutations (map #(.-addedNodes %) mutations)]
        
  ;   (.log js/console "handle-node-ready mutations")
  ;   (.log js/console node)
  ;   (.log js/console  addmutations)
  ;   (when-let [created (first (filter #(= node %) addmutations))]
  ;     (.log js/console  created)
  ;     (go
  ;       (>! chan created))
  ;     ))
  )


(defn add-ready-observer
  [obj]
  (when (:ready @obj)
    (log "add ready observer")
    (let [node (:content @obj)
          ready-chan (chan)
          observer (observe js/document (partial handle-node-ready node ready-chan) :child-list :subtree)]
      ; (go
      ;   (let [created (<! ready-chan)]
      ;     (.disconnect observer)
      ;     (apply (:ready @obj) obj)))
      )))