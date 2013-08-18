(ns think.observe
  (:require [think.util.dom :refer (append $ by-id)]
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
    (js/console.log (reset! last-mutation* mutation))))


(defn handle-summary
  [summaries]
  (doseq [summary summaries]
    (log-obj summary)))


(defn observer [handler]
  (new js/WebKitMutationObserver handler))


(defn init-summary-observer
  ([handler]
    (summary-observer handler [{:all true}]))
  ([handler queries]
    (new js/MutationSummary (clj->js {:callback handler :queries queries}))))


(defn observe
  [node handler & config]
  (.observe (observer handler) node (clj->js (reduce #(assoc %1 (js-style-name (name %2)) true) {} config))))


; (init-summary-observer handle-summary)

(defn add-test-elem []
  (append js/document.body
    (crate.core/html
      [:div#test-elem])))