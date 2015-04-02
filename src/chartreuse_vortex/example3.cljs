;; example 3 modifies the sprite data format. Instead of
;; each sprite data blob holding the x/y/dx/dy at the current
;; time, instead we store the sprites' x/y/dx/dy at a particular time.
;; For a simple app like this the sprite location at future times can
;; be computed for rendering without updating the sprite data structure.
;; This way we avoid updating all our sprite data every frame. Instead
;; specific sprite data is updated only when the sprite bounces off a wall.
;;
;; This representation reduces the number of sprite updates as well as the number
;; of things that get shipped off to om/React. The cost we pay is that updating
;; sprites gets more complicated
;;

(ns chartreuse-vortex.example3
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [chartreuse-vortex.common :as common :refer [*gravity* *spritesize*]]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [>! <! put!]]))

(defonce *maxtimestep* 10)

(defn addsprite [{:keys [sprites] :as appdata} currenttime]
  {:pre [(number? currenttime)]}
  (let [[newx newy] (map rand-int (common/shrinkbyspritesize appdata))
        newsprite {:x newx
                   :y newy
                   :dx 300
                   :dy 0
                   :attime currenttime
                   :key (count sprites)
                   :image common/*spriteimage*}
        newsprites (conj sprites newsprite)]
    (assoc appdata :sprites newsprites)))

(defonce appstate (atom {:width 0
                         :height 0
                         :sprites []}))

(defn linear-hits [x dx targetx maxtime]
  (if (common/float= 0 dx)
    maxtime
    (let [distance (- targetx x)
          time (/ distance dx)]
      (if (< time 0)
        maxtime
        (min time maxtime)))))

(defn ballistic-fall [x y dx dy g elapsedtime]
  "Given a start position, velocity, gravity, and time computes
the new position and velocity after a given time"
  (let [newx (+ x (* dx elapsedtime))
        newy (+ y (* dy elapsedtime) (* 0.5 g elapsedtime elapsedtime))
        newdx dx
        newdy (+ dy (* g elapsedtime))]
    [newx newy newdx newdy]))

(defn advance-sprite [{:keys [x y dx dy attime] :as spritedata} advancetime]
  {:pre [(number? advancetime)
         (number? attime)]}
  "Produce a new spritedata structure by taking the current sprite data
and advancing it in time by a specified amount."
  (let [newtime (+ attime advancetime)
        [newx newy newdx newdy] (ballistic-fall x y dx dy common/*gravity* advancetime)]
    (assoc spritedata
           :x newx
           :y newy
           :dx newdx
           :dy newdy
           :attime newtime)))

(defn parabola-hits [y dy g targetheight maxtime]
  "Given a parabola described by y(t) = y(0) + dy*t + (1/2)g(t*t) this
calculates the non-negative value of t where this parabola reaches the target height. Returns
maxtime if the parabola will never intersect the target height with non-negative t."
  (if (common/float= 0 g)
    ;; if g is zero we can't use the quadratic equation and have to do linear intersection instead
    (linear-hits y dy targetheight maxtime)
    ;; for the quadratic case first check the discriminant to see if it intersects at all
    (let [discriminant (- (* dy dy) (* 2 g (- y targetheight)))]
      ;; If the discriminant is less than zero then root are not real.
      ;; This means that the current parabolic path doesn't intersect the target height.
        (if (>= discriminant 0)
          (let [dissqrt (js/Math.sqrt discriminant)
                minusdy (- dy)
                root1 (/ (- minusdy dissqrt) g)
                root2 (/ (+ minusdy dissqrt) g)]
            ;; root1 will always be the smaller time. Use that one unless
            ;; it is less than zero, which means the intersection happened in the past
            (if (<= 0 root1) root1
                (if (<= 0 root2)
                  root2
                  maxtime)))
          maxtime))))

(defn next-collision-time [{:keys [x y dx dy attime] :as spritedata} width height]
  "Given a sprite figures out at what time the sprite will collide off 
the sides of the display area. Returns a vector [a b] where a is the
collision time and b is a keyword describing the collision type, one of
{:left :right :up :down :none}"
  (let [maxtime *maxtimestep*
        lefthittime (if (>= dx 0)
                      maxtime
                      (/ x (js/Math.abs dx)))
        righthittime (if (<= dx 0)
                       maxtime
                       (/ (- width common/*spritesize* x) dx))
        uphittime (if (<= 0 dy)
                    (parabola-hits y dy *gravity* 0 maxtime)
                    maxtime)
        downhittime (parabola-hits y dy *gravity* (- height common/*spritesize*) maxtime)
        nohittime (- maxtime 0.1)
        ;; consolidate times
        horzhittime (if (< lefthittime righthittime)
                      [lefthittime :left]
                      [righthittime :right])
        verthittime (if (< uphittime downhittime)
                      [uphittime :up]
                      [downhittime :down])
        boundshittime (if (< (first horzhittime) (first verthittime))
                        horzhittime
                        verthittime)
        hittime (if (< nohittime (first boundshittime))
                  [nohittime :none]
                  boundshittime)]
    hittime))

(defn compute-next-sprite-event [spritedata width height]
  "Given sprite data and the screen dimensions produces a new spritedata
data blob that represents the sprite after its next event (collision) occurs."
  (let [[collidetime collidetype] (next-collision-time spritedata width height)
        spritebeforebounce (advance-sprite spritedata collidetime)
        absdx (js/Math.abs (:dx spritebeforebounce))
        absdy (js/Math.abs (:dy spritebeforebounce))]
    (case collidetype
      :left (assoc spritebeforebounce :dx absdx :x 00.01)
      :right (assoc spritebeforebounce :dx (- absdx) :x (- width *spritesize* 0.01))
      :up (assoc spritebeforebounce :dy (* 0.9 absdy) :y 0.01)
      :down (assoc spritebeforebounce :dy (- (* 1 absdy)) :y (- height *spritesize* 0.01))
      :none spritebeforebounce)))

(defn attach-next-event [spritedata width height]
  (if (nil? (get spritedata :nextstate))
    (assoc spritedata :nextstate (compute-next-sprite-event spritedata width height))
    spritedata))

(defn maybe-update-sprite [spritedata nexttime width height]
  {:pre [(number? nexttime)
         (number? width)
         (number? height)]}
  "Possibly updates the sprite data to make sure it is valid at the
time 'nexttime'."
  ;; if the sprite data doesn't contain the next sprite state, add it
  (let [fullspritedata (attach-next-event spritedata width height)
        nextstate (:nextstate fullspritedata)
        nextstatetime (:attime nextstate)]
    (if (> nextstatetime nexttime)
      fullspritedata
      nextstate)))

(defn updateallsprites [{:keys [width height sprites] :as appstate} newtime]
  (let [newsprites (vec (map #(maybe-update-sprite % newtime width height) sprites))]
    (assoc appstate :sprites newsprites)))

(defcomponentk addspritebutton [[:data addspritechannel] owner]
  (display-name [_] "AddSpriteButton")
  (render [_]
          (let [clickhandler (fn [] (put! addspritechannel 1))]
            (pixi/sprite {:x 100 :y 100 :interactive true :image common/*spriteimage* :click clickhandler}))))

(defcomponentk examplestage [[:data width height sprites :as cursor] owner]
  ;; set up (and tear down upon unmount) a recurring calback
  ;; that updates sprites every frame
  (init-state [_] {:addspritechannel (async/chan)})
  (did-mount [_]
             (let [updatefn (fn updatecallback [newtime]
                              (let [faketime (om/get-state owner :faketime)
                                    newfaketime (+ faketime 0.016)]
                                (om/transact! cursor (fn [x] (updateallsprites x newfaketime)))
                                (om/set-state! owner :updatecallback (js/requestAnimationFrame updatecallback))
                                (om/set-state! owner :faketime newfaketime)))
                   addspritechannel (om/get-state owner :addspritechannel)]
               (om/set-state! owner :updatecallback (js/requestAnimationFrame updatefn))
               (om/set-state! owner :faketime 0)
               (om/transact! cursor #(addsprite % 0))
               (go (loop [addclick (<! addspritechannel)]
                     (om/transact! cursor #(addsprite % (om/get-state owner :faketime)))
                     (recur (<! addspritechannel))))))
  (will-unmount [_]
                (when-let [updatefn (om/get-state owner :updatecalback)]
                  (js/cancelAnimationFrame updatefn)))
  (render [_]
          (apply
           pixi/stage
           {:width width :height height :key "stage"}
           (pixi/tilingsprite {:image (common/assetpath-for "bg_castle.png") :width width :height height :key "ack"})
           (om/build addspritebutton {:addspritechannel (om/get-state owner :addspritechannel)})
           (om/build common/spritecountlabel (count sprites))
           (map pixi/sprite sprites)))
  (display-name [_] "ExampleStage3"))

(defn getcomponentandstate []
  {:examplecomponent examplestage
   :exampleappstate appstate})
