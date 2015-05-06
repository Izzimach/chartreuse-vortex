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

(defonce timingdata (atom {:rendertime 0 :basic 0}))

;; from the Clojure Cookbook
(defn- scale [x y]
  (if (or (zero? x) (zero? y))
    1
    (js/Math.abs x)))

(defn float=
  ([x y] (float= x y 0.00001))
  ([x y epsilon] (<= (js/Math.abs (- x y))
                     (* (scale x y) epsilon))))


(defn shrinkbyspritesize [{:keys [width height]}]
  [(- width *spritesize*) (- height *spritesize*)])


(defcomponentk spritecountlabel [data owner]
  (display-name [_] "SpriteCountLabel")
  (render [_]
          (pixi/text {:x 50 :y 50 :text (str "Sprite count: " data)})))


(defcomponentk timinglabels [data owner]
  (display-name [_] "TimingLabels")
  (did-mount [_]
    (let [updatefn (fn updatecallback [_]
                     (om/set-state! owner :perftime (:rendertime @timingdata))
                     (om/set-state! owner :updatecallback (js/requestAnimationFrame updatecallback)))]
      (om/set-state! owner :updatecallback (js/requestAnimationFrame updatefn))))
  (will-unmount [_]
    (when-let [updatefn (om/get-state owner :updatecalback)]
      (js/cancelAnimationFrame updatefn)))
  (render-state [_ state]
    (pixi/text {:x 50 :y 20 :text (str "Time: " (:perftime state))})))
