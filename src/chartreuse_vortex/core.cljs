(ns chartreuse-vortex.core
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [chartreuse-vortex.example1 :as example1]
            [chartreuse-vortex.example2 :as example2]
            [chartreuse-vortex.example3 :as example3]
            [chartreuse-vortex.tests.core :as tests]))


(enable-console-print!)

(defn startchartreuse [{:keys [examplecomponent exampleappstate]}]
  (let [inset #(- % 16)
        w (-> js/window .-innerWidth inset)
        h (-> js/window .-innerHeight inset)]
    (swap! exampleappstate #(-> % (assoc :width w) (assoc :height h)))
    (om/root examplecomponent exampleappstate
             {:target (.getElementById js/document "my-app")})))

(startchartreuse (example1/getcomponentandstate))
