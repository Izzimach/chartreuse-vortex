(ns chartreuse-vortex.core
  (:require [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]))


(enable-console-print!)

(defn assetpath-for [name] (str "../assets/" name))

(def *spriteimage* (assetpath-for "WardenIcon.png"))
(def *spritesize* 128)
(def *gravity* 500)


(defn shrinkbyspritesize [{:keys [width height]}]
  [(- width *spritesize*) (- height *spritesize*)])

(defonce appstate (atom {:width 0
                         :height 0
                         :dt 0.016
                         :sprites [{:x 100 :y 100 :dx 300 :dy 0 :key 0 :image *spriteimage*}]
                         }))

(defn updatesprite [{:keys [x y dx dy key] :as spritedata} dt width height]
  (let [[newx newdx] (let [xp (+ x (* dx dt))]
                       (cond
                         (< xp 0) [0 (js/Math.abs dx)]
                         (> xp width) [width (- (js/Math.abs dx))]
                         :else [xp dx]))
        [newy newdy] (let [yp (+ y (* dy dt))
                           dyp (+ dy (* *gravity* dt))]
                       (cond
                         ;; damp it a little if it hits the top
                         (< yp 0) [0 (js/Math.abs (* 0.8 dyp))]
                         (> yp height) [height (- (js/Math.abs dyp))]
                         :else [yp dyp]))]
    {:x newx :y newy :dx newdx :dy newdy :key key :image *spriteimage*}))

(defn updateallsprites [{:keys [width height sprites dt] :as appstate}]
  (let [[correctedwidth correctedheight] (shrinkbyspritesize appstate)
        newspritedata (map #(updatesprite % dt correctedwidth correctedheight) sprites)]
    (assoc appstate :sprites newspritedata)))

(defn addsprite [{:keys [sprites] :as appdata}]
  (let [[newx newy] (map rand-int (shrinkbyspritesize appdata))
        newsprite {:x newx :y newy :dx 300 :dy 0 :key (count sprites)}
        newsprites (conj sprites newsprite)]
    (assoc appdata :sprites newsprites)))

(defcomponentk addspritebutton [data owner]
  (display-name [_] "AddSpriteButton")
  (render [_]
          (let [clickhandler (fn [] (om/transact! data addsprite))]
            (pixi/sprite {:x 100 :y 100 :interactive true :image (assetpath-for "WardenIcon.png") :click clickhandler})))
  )

(defcomponentk spritecountlabel [data owner]
  (display-name [_] "SpriteCountLabel")
  (render [_]
          (pixi/text {:x 50 :y 50 :text (str "Sprite count: " data)})))

(defcomponentk simplestage [[:data width height sprites :as cursor] owner]
  ;; set up (and tear down upon unmount) a recurring calback
  ;; that updates sprites every frame
  (did-mount [_]
             (let [updatefn (fn updatecallback [_]
                              (om/transact! cursor updateallsprites)
                              (om/set-state! owner :updatecallback (js/requestAnimationFrame updatecallback)))]
               (om/set-state! owner :updatecallback (js/requestAnimationFrame updatefn))))
  (will-unmount [_]
                (when-let [updatefn (om/get-state owner :updatecalback)]
                  (js/cancelAnimationFrame updatefn)))
  (render [_]
          (apply
           pixi/stage
           {:width width :height height :key "stage"}
           (pixi/tilingsprite {:image (assetpath-for "bg_castle.png") :width width :height height :key "ack"})
           (om/build addspritebutton cursor)
           (om/build spritecountlabel (count sprites))
           (map pixi/sprite sprites)))
  (display-name [_] "SimpleStage"))

(defn startchartreuse []
  (let [inset #(- % 16)
        w (-> js/window .-innerWidth inset)
        h (-> js/window .-innerHeight inset)]
    (swap! appstate #(-> % (assoc :width w) (assoc :height h)))
    (om/root simplestage appstate
             {:target (.getElementById js/document "my-app")})))


(startchartreuse)
