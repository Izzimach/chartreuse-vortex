(ns chartreuse-vortex.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [>! <! put!]]))


(defn assetpath-for [name] (str "../assets/" name))

(enable-console-print!)

(defonce appstate (atom {:width 0 :height 0 :sprites [{:image (assetpath-for "WardenIcon.png") :x 100 :y 100}]}))

(omtools/defcomponentk manysprites [data owner]
  (render [_]
          (let [spritedata (nth data 0)]
            (pixi/sprite spritedata))))

(omtools/defcomponentk extrabuttons [data owner]
  (render [_]
          (pixi/sprite {:image (assetpath-for "WardenIcon.png") :x 500 :y 100 :key "morebunnies"})))

(omtools/defcomponentk simplestage [[:data width height sprites] owner]
  (render [_]
          (pixi/stage
           {:width width :height height :key "stage"}
           (pixi/tilingsprite {:image (assetpath-for "bg_castle.png") :width width :height height :key "ack"})
           (om/build manysprites sprites {:react-key "argh"})
           (om/build extrabuttons {} {:react-key "buttons"})))
  (display-name [_] "SimpleStage"))


(defn startchartreuse []
  (let [inset #(- % 16)
        w (-> js/window .-innerWidth inset)
        h (-> js/window .-innerHeight inset)]
    (swap! appstate #(-> % (assoc :width w) (assoc :height h)))
    (om/root simplestage appstate
             {:target (.getElementById js/document "my-app")})))


(startchartreuse)
