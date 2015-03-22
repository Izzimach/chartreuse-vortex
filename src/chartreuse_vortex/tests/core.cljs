(ns chartreuse-vortex.tests.core
  (:require [chartreuse-vortex.example3 :as example3]
            [chartreuse-vortex.common :as common]))

(defn parabola-test []
  (common/float= 1 (example3/parabola-hits 1 -1 0 100 100)))
