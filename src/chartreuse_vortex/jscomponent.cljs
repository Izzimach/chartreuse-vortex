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
  (let [iterobject (js-obj "oldseq" (seq oldchildren) "newseq" (seq newchildren) "iterindex" 0)
        nextfunc (fn []
                   (let [oldseq (aget iterobject "oldseq")
                         oldelement (first oldseq)
                         oldrest (next oldseq)
                         newseq (aget iterobject "newseq")
                         newelement (first newseq)
                         newrest (next newseq)
                         currentindex (aget iterobject "iterindex")
                         nextindex (inc currentindex)
                         op (cond
                              (= nil oldelement newelement) "done"
                              (= nil oldelement)            "append"
                              (= nil newelement)            "remove"
                              (= oldelement newelement)     "noop"
                              :else                         "update")
                         opdata (if (or (= op "noop") (= op "done"))
                                  nil
                                  (clj->js newelement))
                         ]
                     (aset iterobject "oldseq" oldrest)
                     (aset iterobject "newseq" newrest)
                     ;; deleting  at the end basically consists of repeatedly 'remove'-ing
                     ;; at the same index over and over, so don't advance the index when removing
                     (if (not= op "remove")
                       (aset iterobject "iterindex" nextindex))
                     (if (= op "done")
                       #js {:done true :value nil}
                       #js {:done false :value #js {:op op :index currentindex :data opdata}})))]
    (aset iterobject "next" nextfunc)
    iterobject)
  )
