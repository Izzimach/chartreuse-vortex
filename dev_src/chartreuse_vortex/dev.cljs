(ns chartreuse-vortex.dev
  (:require
   [chartreuse-vortex.core]
   [figwheel.client :as fw]))

(fw/start {
           :on-jsload (fn [] (print "Reloaded!"))})

