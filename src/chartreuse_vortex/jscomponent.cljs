(ns chartreuse-vortex.jscomponent
  (:require-macros [chartreuse-vortex.jscomponent :refer [defn-jsvortex-element]])
  (:require [clojure.string :as string]
            [chartreuse-vortex.jsvortex :as jsvortex]))

(defn-jsvortex-element jsvortex/rapidStage)
(defn-jsvortex-element jsvortex/interpolatingSprite)
(defn-jsvortex-element jsvortex/userChildDisplayContainer)

;;
;; standard customChildren comparing iterator
;;

(defn build-customchildren-iterator [oldchildren newchildren]
  ;;
  ;; this is basically a javascript iterator
  ;;
  (let [iterobject (js-obj "oldseq" (seq oldchildren) "newseq" (seq newchildren))
        nextfunc (fn []
                   (let [oldseq (aget iterobject "oldseq")
                         oldelement (first oldseq)
                         oldrest (rest oldseq)
                         newseq (aget iterobject "newseq")
                         newelement (first newseq)
                         newrest (rest newseq)
                         ]
                     (aset iterobject "oldseq" oldrest)
                     (aset iterobject "newseq" newrest)
                     #_(if (not= nil oldelement newelement)
                       (.log js/console (clj->js oldelement) (clj->js newelement) (= oldelement newelement))
                       )
                     (cond
                       (= nil oldelement newelement) #js {:done true :value nil}
                       (= nil oldelement) #js {:done false :value "append"}
                       (= nil newelement) #js {:done false :value "remove"}
                       (= oldelement newelement) #js {:done false :value "noop"}
                       :else #js{:done false :value "update"})))]
    (aset iterobject "next" nextfunc)
    iterobject)
  )
