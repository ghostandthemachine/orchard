(ns think.objects.editor.pool
  (:require [think.object :as object]
            [think.objects.app :as app]
            [think.objects.canvas :as canvas]
            ; [think.objects.editor :as editor]
            [think.util.cljs :refer [->dottedkw]]
            [think.util.dom :as dom])
  (:require-macros [think.macros :refer [defui]]))

(defn get-all []
  (object/instances-by-type editor/ed-obj))

(object/behavior* ::theme-changed
                  :triggers #{:theme-change}
                  :reaction (fn [this theme]
                              (doseq [ed (get-all)
                                      :let [e (:ed @ed)]]
                                (editor/set-options e {:theme theme}))))

(object/behavior* ::line-numbers-changed
                  :triggers #{:line-numbers-change}
                  :reaction (fn [this numbers?]
                              (doseq [ed (get-all)
                                      :let [e (:ed @ed)]]
                                (editor/set-options e {:lineNumbers numbers?}))))

(object/behavior* ::options-changed
                  :triggers #{:options-changed}
                  :reaction (fn [this opts]
                              (doseq [ed (get-all)
                                      :let [e (:ed @ed)]]
                                (editor/set-options e opts))))

(object/object* ::pool
                :tags #{:editor.pool})

(object/tag-behaviors :editor.pool [::theme-changed ::line-numbers-changed ::options-changed])

(defn unsaved? []
  (some #(:dirty (deref %)) (object/instances-by-type editor/ed-obj)))

(defn by-path [path]
  (filter #(= (-> @% :info :path) path) (object/by-tag :editor)))

(defui button [label & [cb]]
       [:div.button.right label]
       :click (fn []
                (when cb
                  (cb))))

; (defn unsaved-prompt [on-yes]
;   (popup/popup! {:header "You will lose changes."
;                  :body "If you close now, you'll lose any unsaved changes. Are you sure you want to do that?"
;                  :buttons [{:label "Discard changes"
;                             :action on-yes}
;                            popup/cancel-button]}))

(def pool (object/create ::pool))

(object/behavior* ::track-active
                  :triggers #{:active}
                  :reaction (fn [this]
                              (object/merge! pool {:last this})))

(object/behavior* ::stop-close-dirty
                  :triggers #{:close}
                  :reaction (fn [this]
                              (when (unsaved?)
                                (app/prevent-close)
                                (unsaved-prompt (partial app/close true)))))

(object/behavior* ::stop-reload-dirty
                  :triggers #{:reload}
                  :reaction (fn [this]
                              (when (unsaved?)
                                (app/prevent-close)
                                (unsaved-prompt app/refresh))))

(object/behavior* ::ed-close
                  :triggers #{:close}
                  :reaction (fn [this]
                              (if (:dirty @this)
                                (unsaved-prompt #(object/raise this :close.force))
                                (object/raise this :close.force))))

(object/behavior* ::focus-last-on-focus
                  :triggers #{:focus!}
                  :reaction (fn [this]
                              (focus-last)))

(defn last-active []
  (:last @pool))

(defn focus-last []
  (when-let [ed (last-active)]
    (when-let [ed (:ed @ed)]
      (dom/focus js/document.body)
      (editor/focus ed))))

(defn set-syntax [ed mode]
  (let [prev-type (-> @ed :info :type)]
    (object/update! ed [:info] assoc :type mode)
    (editor/set-mode ed mode)
    (when prev-type
      (object/remove-tags ed [(->dottedkw :editor prev-type)]))
    (object/add-tags ed [(->dottedkw :editor mode)])))

(defn create [info & [wraps]]
  (let [wraps (apply comp wraps)
        ed (object/create (-> editor/ed-obj wraps) info)]
    (object/add-tags ed [(->dottedkw :editor (:type info))])
    (object/raise pool :create ed info)
    ed))

(object/add-behavior! canvas/canvas ::focus-last-on-focus)
(object/tag-behaviors :app [::stop-close-dirty ::stop-reload-dirty])
(object/tag-behaviors :editor [::ed-close ::track-active])

(def default-tab-settings {:indentWithTabs false
                           :indentUnit 2
                           :tabSize 2})
(def tab-size (cmd/options-input {:placeholder "Tab size"}))
(def indent-unit (cmd/options-input {:placeholder "Indent unit"}))
(def use-tabs (cmd/filter-list {:items [{:item "true" :value true} {:item "false" :value false}]
                                :key :item
                                :set-on-select true
                                :placeholder "Use tabs?"}))

(object/object* ::tab-options
                :tags #{:tab-options}
                :init (fn [this]
                        [:div.tab-settings
                         [:label "Tab size (width of a tab character)"]
                         (object/->content tab-size)
                         [:label "Indent unit (spaces per indent)"]
                         (object/->content indent-unit)
                         [:label "Indent with tabs?"]
                         (object/->content use-tabs)]))

(object/behavior* ::focus-options
                  :triggers #{:focus!}
                  :reaction (fn [this]
                              (object/raise tab-size :focus!)))

(object/behavior* ::set-tab-settings
                  :triggers #{:select}
                  :reaction (fn [this v]
                              (cmd/exec-active! {:indentWithTabs (:value (cmd/current-selected use-tabs))
                                                 :indentUnit (js/parseInt (dom/val (object/->content indent-unit)))
                                                 :tabSize (js/parseInt (dom/val (object/->content tab-size)))
                                                 })))

(object/behavior* ::add-tab-settings
                  :triggers #{:create}
                  :reaction (fn [this ed]
                              (let [stts (or (settings/fetch :tab-settings) default-tab-settings)]
                                (editor/set-options ed stts))
                              ))

(object/behavior* ::init-tab-settings
                  :triggers #{:init}
                  :reaction (fn [this]
                              (let [stts (or (settings/fetch :tab-settings) default-tab-settings)]
                                (object/merge! tab-size {:value (:tabSize stts)})
                                (object/merge! indent-unit {:value (:indentUnit stts)})
                                (cmd/set-and-select use-tabs (if (:indentWithTabs stts)
                                                               "true"
                                                               "false")))))

(object/add-behavior! tab-size ::set-tab-settings)
(object/add-behavior! indent-unit ::set-tab-settings)
(object/add-behavior! use-tabs ::set-tab-settings)
(object/tag-behaviors :tab-options [::focus-options])
(object/tag-behaviors :editor.pool [::add-tab-settings])
(object/tag-behaviors :app [::init-tab-settings])

; (cmd/command {:command :change-tab-settings
;               :desc "Set tab size/behavior"
;               :options (object/create ::tab-options)
;               :exec (fn [opts]
;                       (settings/store! :tab-settings opts)
;                       (object/raise pool :options-changed opts)
;                       )})

; (cmd/command {:command :focus-last-editor
;               :desc "Focus last active editor"
;               :exec (fn []
;                       (focus-last))})

; (def syntaxes [{:item "CSS" :mode "css"}
; {:item "HTML" :mode "htmlmixed"}
; {:item "Javascript" :mode "javascript"}
; {:item "JSON" :mode "javascript"}
; {:item "C" :mode "text/x-c"}
; {:item "C++" :mode "text/x-c++src"}
; {:item "C++ Header" :mode "text/x-c++hdr"}
; {:item "Java" :mode "text/x-java"}
; {:item "C#" :mode "text/x-csharp"}
; {:item "Scala" :mode "text/x-scala"}
; {:item "CoffeeScript" :mode "text/x-coffeescript"}
; {:item "Common Lisp" :mode "text/x-common-lisp"}
; {:item "Diff" :mode "text/x-diff"}
; {:item "Patch" :mode "text/x-diff"}
; {:item "Erlang" :mode "text/x-erlang"}
; {:item "Go" :mode "text/x-go"}
; {:item "Groovy" :mode "text/x-groovy"}
; {:item "Haskell" :mode "text/x-haskell"}
; {:item "Haxe" :mode "text/x-haxe"}
; {:item "LESS" :mode "text/x-less"}
; {:item "Lua" :mode "text/x-lua"}
; {:item "OCaml" :mode "text/x-ocaml"}
; {:item "Pascal" :mode "text/x-pascal"}
; {:item "Perl" :mode "text/x-perl"}
; {:item "PHP" :mode "text/x-php"}
; {:item "SQL" :mode "text/x-plsql"}
; {:item "Ini" :mode "text/x-ini"}
; {:item "R" :mode "text/x-rsrc"}
; {:item "Rust" :mode "text/x-rustsrc"}
; {:item "LaTeX" :mode "text/x-stex"}
; {:item "Scheme" :mode "text/x-scheme"}
; {:item "Shell" :mode "text/x-sh"}
;                {:item "SCSS" :mode "text/x-scss"}
; {:item "St" :mode "text/x-stsrc"}
; {:item "Smarty" :mode "text/x-smarty"}
; {:item "SPARQL" :mode "text/x-sparql-query"}
; {:item "Visual Basic" :mode "text/x-vb"}
;                {:item "XML" :mode "text/x-xml"}
; {:item "Ruby" :mode "ruby"}
; {:item "Clojure" :mode "clj"}
; {:item "ClojureScript" :mode "cljs"}
; {:item "Yaml" :mode "yaml"}
; {:item "Python" :mode "python"}
; {:item "Markdown" :mode "markdown"}
; ])

; (def syntax-selector (cmd/filter-list {:items syntaxes
;                                        :key :item
;                                        :placeholder "Syntax"}))

; (object/behavior* ::set-syntax
;                   :triggers #{:select}
;                   :reaction (fn [this v]
;                               (cmd/exec-active! v)))

; (object/add-behavior! syntax-selector ::set-syntax)

; (cmd/command {:command :set-syntax
;               :desc "Set editor syntax"
;               :options syntax-selector
;               :exec (fn [syn]
;                       (set-syntax (last-active) (:mode syn)))})
