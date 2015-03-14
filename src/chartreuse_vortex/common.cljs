;;
;; Constants and functions used by all the examples.
;;

(ns chartreuse-vortex.common
  (:require [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]))

(defn assetpath-for [name] (str "../assets/" name))

(def *spriteimage* (assetpath-for "WardenIcon.png"))
(def *spritesize* 128)
(def *gravity* 500)


(defn shrinkbyspritesize [{:keys [width height]}]
  [(- width *spritesize*) (- height *spritesize*)])


(defcomponentk spritecountlabel [data owner]
  (display-name [_] "SpriteCountLabel")
  (render [_]
          (pixi/text {:x 50 :y 50 :text (str "Sprite count: " data)})))

