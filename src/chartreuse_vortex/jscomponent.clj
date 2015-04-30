(ns chartreuse-vortex.jscomponent)

;; macros to build/refer to jsvortex components
;; these use the same form as omreactpixi.abbrev components

(defmacro defn-jsvortex-element [tag]
  (let [jsname  (symbol "js" (str "chartreuse_vortex.jsvortex." (name tag)))
        cljname (symbol (clojure.string/lower-case (name tag)))]
    `(defn ~cljname [opts# & children#]
       (let [[opts# children#] (omreactpixi.abbrev.element-args opts# children#)]
         (apply React.createElement
                ~jsname
                (cljs.core/into-array (cons opts# children#)))))))


