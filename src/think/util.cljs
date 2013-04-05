(ns think.util)

(defn uuid []
  (let [gen-val (fn [] (.toString (Math/floor (* (Math/random) 0x10000)) 16))]
    (str (gen-val) (gen-val) "-"
         (gen-val) "-"
         (gen-val) "-"
         (gen-val) "-"
         (gen-val) (gen-val) (gen-val))))

(defn log [& m]
  (.log js/console (apply str m)))

(defn jslog [& m]
  (log (apply js->clj m)))

(defn toJSON [o]
  (let [o (if (map? o) (clj->js o) o)]
    (.stringify (.-JSON js/window) o)))

(defn parseJSON [x]
  (.parse (.-JSON js/window) x))

(defn module-selector
  ([module-id]
    (module-selector module-id ".module-row"))
  ([module-class module-id]
    (str module-class "[data-module-id=\"" module-id "\"]")))

(defn as-str
  ([] "")
  ([x]
    ; TODO: Maybe use something like (satisfies? INamed x) instead?
    (if (or (symbol? x) (keyword? x))
      (name x)
      (str x)))
  ([x & xs]
    ((fn [s more]
       (if more
         (recur (str s (as-str (first more))) (next more))
         s))
     (as-str x) xs)))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (-> (as-str text)
    (str/replace "&"  "&amp;")
    (str/replace "<"  "&lt;")
    (str/replace ">"  "&gt;")
    (str/replace "\"" "&quot;")))