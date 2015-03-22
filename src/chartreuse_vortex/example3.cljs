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
  (:require [chartreuse-vortex.common :as common :refer (*gravity*)]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]))

(defn addsprite [{:keys [sprites] :as appdata} currenttime]
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

(defonce appstate (atom (addsprite {:width 0
                                    :height 0
                                    :dt 0.016
                                    :sprites []}
                                   0)))

(defn linear-hits [x dx targetx maxtime]
  (if (common/float= 0 dx)
    maxtime
    (let [distance (- targetx x)
          time (/ distance dx)]
      (if (< time 0)
        maxtime
        (min time maxtime)))))

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
            (if (<= 0 root1) root1  root2))
          maxtime))))

(defn next-collision-time [{:keys [x y dx dy attime] :as spritedata} width height]
  "Given a sprite figures out at what time the sprite will collide off 
the sides of the display area"
  (let [maxtime 300000
        lefhittime (if (>= dx 0)
                  maxtime
                  (/ x (js/Math.abs dx)))
        righthittime (if (<= dx 0)
                    maxtime
                    (/ (- width x) dx))
        uphittime (if (<= 0 dy)
                 (parabola-hits y dy *gravity* 0 maxtime)
                 maxtime)
        downhittime (parabola-hits y dy *gravity* height maxtime)
        nohittime (- maxtime 1)]
    
     ))

(defn updatespritetonextevent [{:keys [x y dx dy key attime] :as spritedata} width height]
  (let []))
