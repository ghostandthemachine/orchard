(ns think.objects.modules.markdown
  (:use-macros [think.macros :only [defui]])
  (:require [think.object :as object]
            [dommy.template :as tpl]
            [think.util.log :refer [log]]
            [crate.binding :refer [bound subatom]]))


; (defui module-btn
;   [this]
;   [:div.module-btn]
;   :click (fn [e] (object/raise :toggle-module this)))

; (defui module
;   [this m]
;   [:div.module
;     (module-btn this)
;     [:div.module-content
;       (:module this)]])

; ;;*********************************************************
; ;; Creating
; ;;*********************************************************
; (def default-opts
;   (clj->js
;     {:mode "markdown"
;      :theme "default"
;      :lineNumbers true
;      :tabMode "indent"
;      :autofocus true
;      :linewrapping true}))

; (def ed-id (atom 0))

; (defn ed-with-elem [$elem opts]
;   (js/CodeMirror (if (.-get $elem)
;                             (.get $elem 0)
;                             $elem)
;                  (clj->js opts)))



; (defn ed-headless [opts]
;   (-> (js/CodeMirror. (fn []))
;       (set-options opts)))

; (defn ->editor [$elem opts]
;   (let [ed (if $elem
;              (ed-with-elem $elem opts)
;              (ed-headless opts))]
;     (set! (.-thinkid ed) (swap! ed-id inc))
;     (set! (.-thinkproperties ed) (atom {}))
;     ed))

; (defn init [ed context]
;   (-> ed
;       (clear-props)
;       (set-props (dissoc context :content))
;       (set-val (:content context))
;       (set-options {:mode (name (:type context))
;                     :readOnly false
;                     :dragDrop false
;                     :lineNumbers false
;                     :lineWrapping false})))

; (defn make [$elem context]
;   (let [e (->editor $elem {:mode (if (:type context)
;                                    (name (:type context))
;                                    "text")
;                            :autoClearEmptyLines true
;                            :tabSize 2
;                            :indentUnit 2
;                            :indentWithTabs false
;                            :dragDrop false
;                            :onDragEvent (fn [] true)
;                            :lineNumbers false
;                            :undoDepth 10000
;                            :matchBrackets true})]
;     (set-props e (dissoc context :content))
;     (when-let [c (:content context)]
;       (set-val e c)
;       (.clearHistory e))
;     e))

; (defn ->cm-ed [e]
;   (if (satisfies? IDeref e)
;     (:ed @e)
;     e))

; (defn on [ed ev func]
;   (.on (->cm-ed ed) (name ev) func))

; (defn off [ed ev func]
;   (.off (->cm-ed ed) (name ev) func))

; (defn wrap-object-events [ed obj]
;   (on ed :scroll #(object/raise obj :scroll %))
;   (on ed :update #(object/raise obj :update % %2))
;   (on ed :change #(object/raise obj :change % %2))
;   (on ed :cursorActivity #(object/raise obj :move % %2))
;   (on ed :focus #(object/raise obj :focus %))
;   (on ed :blur #(object/raise obj :blur %)))


; (defn create-code-mirror
;   [editor-id]
;   (CodeMirror/fromTextArea
;     (sel1 (str "#editor-" editor-id))
;     default-opts))


; (defn syntax-highlighted-editor-module
;   [record]
;   (let [module (module
;                   [:div.module-editor
;                     [:textarea {:id (str "editor-" (:id record))}]] record)
;         id     (:id record)]
;     (dom/replace! (sel1 (str "#" id))
;       module)
;     (let [cm-instance (create-code-mirror id)]
;       (.setValue cm-instance (:text record)))))



; (object/behavior* ::toggle-module
;                   :triggers #{:toggle}
;                   :reaction (fn [this]
;                               (case (:mode this)
;                                 :present (object/merge! this {:module (syntax-highlighted-editor-module this)
;                                                               :mode   :edit})
;                                 :edit    (object/merge! this {:module (present-module this)
;                                                               :mode   :present}))))

(defui render-markdown
  [this text]
  [:div.markdown
    ; (let [html (js/markdown.toHTML text)]
    ;   (tpl/html->nodes html))
    [:h1 "should be a module"]
    ])

(object/object* ::markdown-module
                :tags #{}
                :mode :present
                :init (fn [this record]
                        (let [this (object/merge! this record)]
                          [:div.markdown-module
                            ; (bound (subatom this [:text]) (partial render-markdown this))
                            ])))

(defn create
  [record]
  (object/create ::markdown-module record))
