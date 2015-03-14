(ns chartreuse-vortex.core
  (:require [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]
            [chartreuse-vortex.example1 :as example1]))


(enable-console-print!)

(defn startchartreuse []
  (let [inset #(- % 16)
        w (-> js/window .-innerWidth inset)
        h (-> js/window .-innerHeight inset)]
    (swap! example1/appstate #(-> % (assoc :width w) (assoc :height h)))
    (om/root example1/examplestage example1/appstate
             {:target (.getElementById js/document "my-app")})))


(startchartreuse)
