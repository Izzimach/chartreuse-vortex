;;
;; example 1 - the worst possible way
;; Just update the data structures like you would update mutable state.
;; This method is slow but also does not take advantage of the efficiency
;; afforded by structural sharing of immutable data structures.
;;
(ns chartreuse-vortex.example1
  (:require [chartreuse-vortex.common :as common :include-macros true]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]
            [chartreuse-vortex.common :as common :include-macros true]))

(defn addsprite [{:keys [sprites] :as appdata}]
  (let [[newx newy] (map rand-int (common/shrinkbyspritesize appdata))
        newsprite {:x newx :y newy :dx 300 :dy 0 :key (count sprites) :image common/*spriteimage*}
        newsprites (conj sprites newsprite)]
    (assoc appdata :sprites newsprites)))

(defonce appstate (atom (addsprite {:width 0
                                    :height 0
                                    :dt 0.016
                                    :sprites []})))

(defn updatesprite [{:keys [x y dx dy key] :as spritedata} dt width height]
  ;; typical forward euler method:
  ;; p_x' = p_x + v_x * dt
  ;; p_y' = p_y + v_y * dt
  ;; v_x' = v_x
  ;; v_y' = v_y +   g * dt
  ;; plus a cond to handle bouncing off the sides
  (let [[newx newdx] (let [xp (+ x (* dx dt))]
                       (cond
                         (< xp 0) [0 (js/Math.abs dx)]
                         (> xp width) [width (- (js/Math.abs dx))]
                         :else [xp dx]))
        [newy newdy] (let [yp (+ y (* dy dt))
                           dyp (+ dy (* common/*gravity* dt))]
                       (cond
                         ;; damp it a little if it hits the top
                         (< yp 0) [0 (js/Math.abs (* 0.8 dyp))]
                         (> yp height) [height (- (js/Math.abs dyp))]
                         :else [yp dyp]))]
    {:x newx :y newy :dx newdx :dy newdy :key key :image common/*spriteimage*}))

(defn updateallsprites [{:keys [width height sprites dt] :as appstate}]
  (let [[correctedwidth correctedheight] (common/shrinkbyspritesize appstate)
        newspritedata (map #(updatesprite % dt correctedwidth correctedheight) sprites)]
    (assoc appstate :sprites newspritedata)))


(defcomponentk addspritebutton [data owner]
  (display-name [_] "AddSpriteButton")
  (render [_]
          (let [clickhandler (fn [] (om/transact! data addsprite))]
            (pixi/sprite {:x 100 :y 100 :interactive true :image common/*spriteimage* :click clickhandler}))))

(defcomponentk examplestage [[:data width height sprites :as cursor] owner]
  ;; set up (and tear down upon unmount) a recurring calback
  ;; that updates sprites every frame
  (did-mount [_]
             (let [updatefn (fn updatecallback [_]
                              (common/time-sexp :updatetime
                                (om/transact! cursor updateallsprites))
                              (om/set-state! owner :updatecallback (js/requestAnimationFrame updatecallback)))]
               (om/set-state! owner :updatecallback (js/requestAnimationFrame updatefn))))
  (will-unmount [_]
                (when-let [updatefn (om/get-state owner :updatecalback)]
                  (js/cancelAnimationFrame updatefn)))
  (render [_]
    (common/time-sexp :rendertime
      (apply
        pixi/stage
        {:width width :height height :key "stage"}
        (pixi/tilingsprite {:image (common/assetpath-for "bg_castle.png") :width width :height height :key "ack"})
        (om/build addspritebutton cursor)
        (map pixi/sprite sprites))))

  (display-name [_] "SimpleStage"))

(defn getcomponentandstate []
  {:examplecomponent examplestage
   :exampleappstate appstate
   })
