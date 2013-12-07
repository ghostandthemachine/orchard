(ns orchard.editable.core
  (:require-macros
    [cljs.core.async.macros :refer [go]])
  (:require [orchard.util.dom :as dom]
            [crate.core :as crate]
            [orchard.util.log  :refer (log log-obj)]
            [orchard.util.core :refer (js-style-name)]
            [cljs.core.async :refer [chan >! <! timeout put! get!]]))


(defn has?
  [coll k]
  (not (nil? (some #{k} coll))))


(defn on
  [el ev f]
  (aset el (str "on" (name ev)) f))

(defn event-chan
  [el ev]
  (let [c (chan)]
    (on el ev
      (fn [e]
        (go (>! c e))))
    c))

(defn click-chan
  [sel]
  (event-chan (dom/$ sel) :click))



(defn find-nodes
  [element]
  (loop [el (aget element "parentNode") nodes []]
    (if-let [p (aget el "parentNode")]
      (recur (aget el "parentNode") (distinct (conj nodes (aget el "nodeName"))))
      ;; drop '(HTML BODY ...)
      (drop 2 nodes))))


(defn selection []
  (.getSelection js/document))


(defn selected-text
  "While selection returns a selection object, this function just returns the text selected."
  []
  (let [sel (selection)
        range-count (aget sel "rangeCount")]
    (when range-count
      (loop [i 0 container (crate/html [:div])]
        (if (< i range-count)
          (let [nxt (.cloneContents (.getRangeAt sel i))]
            (recur (inc i) (dom/append container nxt)))
          (aget container "innerHTML"))))))


(defn focus-node []
  (aget (selection) "focusNode"))


(defn toolbar-state
  "Takes a coll of node names which represent the toolbar btn names which should be active or not based on
  selection node's tree. Returns a coll of the elements present in the current focus."
  [btns]
  (let [cur-nodes (find-nodes (focus-node))]
    ))

(defn set-attr
  [el & args]
  (let [args (partition 2 args)]
    (doseq [[k v] args]
      (.setAttribute el (name k) v))))


(defn make-editable!
  ([el]
    (make-editable! el true))
  ([el b]
    (.setAttribute el "designMode" "on")
    (.setAttribute el "contenteditable" b)))


(defn get-selection
  []
  (.getSelection js/window))


(defn create-range
  []
  (.createRange js/document))


(defn exec-cmd
  "String  cmd  - the name of the command
   Boolean ui?  - whether the default user interface should be shown. This is not implemented in Mozilla.
   String  v    - some commands (such as insertimage) require an extra value argument (the image's url). Pass an argument of null if no argument is needed."
  ([cmd]
    (exec-cmd cmd false nil))
  ([cmd v]
    (exec-cmd cmd false v))
  ([cmd ui? v]
    (.execCommand js/document (js-style-name (name cmd)) ui? v)))


(defn justify-selection
  "Justify selection. Options are [:left :right :center, :full]"
  [type]
  (exec-cmd (str "justify-" (name type)) false))


(defn apply-attribute
  "Handles toggling off and on styles for selected text. Options are [:bold :italic :strike-through :underline :subscript :superscript]"
  [type]
  (exec-cmd type))


(defn find-tag-name
  [tag]
  (loop [synonyms [["horizontal-rule"  "hr"]
                   ["ordered-list"     "ol"]
                   ["unordered-list"   "ul"]
                   ["paragraph"        "p" ]
                   ["blockquote"       "quote"]
                   ["image"            "img"]]]
    (if synonyms
      (let [tags (first synonyms)]
        (if (has? tags (name tag))
          (first tags)
          (recur (rest synonyms))))
      (name tag))))


(defn insert-element
  "Insert an element at the current caret position. Tags options in include :hr, :ol, :ul, :p, :quote, :img or the full name (i.e. :hr => :horizontal-rule)."
  [tag]
  (exec-cmd (str "insert-" (find-tag-name tag)) false))


(defn format-block
  "Adds an HTML block-style tag around the line containing the current selection, replacing the block element containing the line if one exists (in Firefox, BLOCKQUOTE is the exception - it will wrap any containing block element). Requires a tag-name string to be passed in as a value argument. Virtually all block style tags can be used (eg. \"H1\", \"P\", \"DL\", \"BLOCKQUOTE\")."
  [tag & v]
  (exec-cmd :format-block (name tag)))


(defn create-link
  "Takes the href to link to."
  [href]
  (exec-cmd :create-link href))


(defn underline-selection []
  (exec-cmd :underline))


(defn strike-through-selection []
  (exec-cmd :strike-through))


(defn selected-node []
  (if (.-selection js/document)
    (let [s (.-selection js/document)
          r (.createRange s)]
      (.parentElement r))
    (let [s (.getSelection js/window)]
      (if (> (.-rangeCount s) 0)
        (let [r (.getRangeAt s 0)
              c (.-startContainer r)]
          (.-parentNode c))
        nil))))


(defn restore-selection
  [rng]
  (cond
    (aget js/window "getSelection")
      (let [sel (.getSelection js/window)]
        (.removeAllRanges sel)
        (.addRange sel rng))
    ;; IE
    (and (aget js/document "selection") (aget rng "select"))
      (.select rng)))

(defn save-selection []
  (cond
    (aget js/window "getSelection")
      (let [sel (.getSelection js/window)]
        (if (> (aget sel "rangeCount") 0)
          (.getRangeAt sel 0)))
    ;; IE
    (and (aget js/document "selection") (aget (aget js/document "selection") "createRange"))
      (let [sel (aget js/document "selection")]
        (.createRange sel))))


(defn set-cursor
  [pos el]
  (if (aget js/document "createRange")
    (let [rng         (create-range)
          sel         (get-selection)
          last-child  ()])
    ;;IE
    (let [body (aget js/document "body")
          rng  (.createTextRange body)]
      (.moveToElementText rng el)
      (.collapse rng false)
      (.select rng))))