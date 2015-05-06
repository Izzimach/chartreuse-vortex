;;
;; common macros . really just the timing macro for now
;;

(ns chartreuse-vortex.common)

(defmacro time-sexp
  "Times the the code in body and stores the time in an atom (map) common/timingdata under
  the specified keyword"
  [keyword body]
  `(let [starttime# (.now js/performance)]
     (let [result# ~body]
       (let [endtime# (.now js/performance)
             perftime# (- endtime# starttime#)]
         (swap! chartreuse-vortex.common/timingdata #(assoc % ~keyword perftime#))
         result#))))
