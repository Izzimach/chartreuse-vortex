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
  (:require [chartreuse-vortex.common :as common :refer [*gravity* *spritesize*] :include-macros true]
            [goog.events :as events]
            [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            ;; we use element-args for our own macro here
            [omreactpixi.abbrev :as pixi]
            [schema.core :as schema]
            [clojure.string :as string]
            [cljs.core.async :as async :refer [>! <! put!]]
            ;; this directly imports js code from the :libs path...
            [chartreuse-vortex.jscomponent :as jscomponent :include-macros true]
            [chartreuse-vortex.jsvortex :as jsvortex]))

(defonce *maxtimestep* 10)

;;
;; need to code up clojurescript shims for our custom components
;;

(defn addsprite [{:keys [sprites] :as appdata} currenttime]
  {:pre [(number? currenttime)]}
  (let [[newx newy] (map rand-int (common/shrinkbyspritesize appdata))
        newdx (+ 150 (rand-int 300))
        newsprite {:x0    newx
                   :y0    (/ newy 2)
                   :dx0   newdx
                   :dy0   0
                   :ddx0  0
                   :ddy0  common/*gravity*
                   :t0    currenttime
                   :key   (count sprites)
                   :image common/*spriteimage*}
        newsprites (conj sprites newsprite)]
    (assoc appdata :sprites newsprites)))

(defn clearsprites [appdata]
  (assoc appdata :sprites []))

(defonce appstate (atom {:width 0
                         :height 0
                         :sprites []}))

(defn linear-hits [x dx targetx maxtime]
  {:pre [(number? x)
         (number? dx)
         (number? targetx)
         (number? maxtime)]}
  (if (common/float= 0 dx)
    maxtime
    (let [distance (- targetx x)
          time (/ distance dx)]
      (if (< time 0)
        maxtime
        (min time maxtime)))))

(defn constant-accelerate [x y dx dy ddx ddy elapsedtime]
  {:pre [(number? x)
         (number? y)]}
  "Given a Start position, velocity, gravity, and time computes
the new position and velocity after a given time"
  (let [newx (+ x (* dx elapsedtime) (* 0.5 ddx elapsedtime elapsedtime))
        newy (+ y (* dy elapsedtime) (* 0.5 ddy elapsedtime elapsedtime))
        newdx (+ dx (* ddx elapsedtime))
        newdy (+ dy (* ddy elapsedtime))]
    [newx newy newdx newdy]))

(defn advance-sprite [{:keys [x0 y0 dx0 dy0 ddx0 ddy0 t0] :as spritedata} advancetime]
  {:pre [(number? advancetime)
         (number? t0)]}
  "Produce a new spritedata structure by taking the current sprite data
and advancing it in time by a specified amount."
  (let [newtime (+ t0 advancetime)
        [newx newy newdx newdy] (constant-accelerate x0 y0 dx0 dy0 ddx0 ddy0 advancetime)]
    (assoc spritedata
           :x0 newx
           :y0 newy
           :dx0 newdx
           :dy0 newdy
	   ;; don't modify accelerations here
           :t0 newtime)))

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

(defn next-collision-time [{:keys [x0 y0 dx0 dy0 t0] :as spritedata} width height]
  "Given a sprite figures out at what time the sprite will collide off 
the sides of the display area. Returns a vector [a b] where a is the
collision time and b is a keyword describing the collision type, one of
{:left :right :up :down :none}"
  (let [maxtime *maxtimestep*
        lefthittime (if (>= dx0 0)
                      maxtime
                      (/ x0 (js/Math.abs dx0)))
        righthittime (if (<= dx0 0)
                       maxtime
                       (/ (- width common/*spritesize* x0) dx0))
        uphittime (if (<= 0 dy0)
                    (parabola-hits y0 dy0 *gravity* 0 maxtime)
                    maxtime)
        downhittime (parabola-hits y0 dy0 *gravity* (- height common/*spritesize*) maxtime)
        nohittime (- maxtime 0.001)
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
        absdx (js/Math.abs (:dx0 spritebeforebounce))
        absdy (js/Math.abs (:dy0 spritebeforebounce))]
    (case collidetype
      :left (assoc spritebeforebounce :dx0 absdx :x0 00.01)
      :right (assoc spritebeforebounce :dx0 (- absdx) :x0 (- width *spritesize* 0.01))
      :up (assoc spritebeforebounce :dy0 (* 0.9 absdy) :y0 0.01)
      :down (assoc spritebeforebounce :dy0 (- (* 1 absdy)) :y0 (- height *spritesize* 0.01))
      :none spritebeforebounce)))

(defn attach-next-event [spritedata width height]
  (if (nil? (get spritedata :nextstate))
    (assoc spritedata :nextstate (compute-next-sprite-event spritedata width height))
    spritedata))

(defn maybe-update-sprite [spritedata nexttime width height]
  {:pre [(number? nexttime)
         (number? width)
         (number? height)]}
  "Possibly updates the sprite data to make sure it is valid at the time 'nexttime'."
  ;; if the sprite data doesn't contain the next sprite state, add it
  (let [fullspritedata (attach-next-event spritedata width height)
        nextstate (:nextstate fullspritedata)
        nextstatetime (:t0 nextstate)]
    (if (> nextstatetime nexttime)
      fullspritedata
      nextstate)))

(defn updateallsprites [{:keys [width height sprites] :as appstate} newtime]
  (let [newsprites (vec (map #(maybe-update-sprite % newtime width height) sprites))]
    (assoc appstate :sprites newsprites)))

(defcomponentk examplestage [[:data width height sprites :as cursor] owner]
  ;; set up (and tear down upon unmount) a recurring calback
  ;; that updates sprites every frame
  (init-state [_] {:spritecontrolchannel (async/chan 1)})
  (did-mount [_]
             (let [updatefn (fn updatecallback [newtime]
                              (let [faketime (om/get-state owner :faketime)
                                    newfaketime (+ faketime 0.016)]
                                (om/transact! cursor (fn [x] (common/time-sexp :updatetime (updateallsprites x newfaketime))))
                                (om/set-state! owner :updatecallback (js/requestAnimationFrame updatecallback))
                                (om/set-state! owner :faketime newfaketime)))
                   spritecontrolchannel (om/get-state owner :spritecontrolchannel)
                   addspritefunc (fn [appdata] (addsprite appdata (om/get-state owner :faketime)))]
               (om/set-state! owner :updatecallback (js/requestAnimationFrame updatefn))
               (om/set-state! owner :faketime 0)
               (om/transact! cursor #(addsprite % 0))
               (go (loop [clickdata (<! spritecontrolchannel)]
                     (cond
                       (= clickdata "plus")    (om/transact! cursor addspritefunc)
                       (= clickdata "minus")   (om/transact! cursor clearsprites)
                       :else                   (.warn js/console (str "Illegal data in spritecontrolchannel:" clickdata)))
                     (recur (<! spritecontrolchannel))))))
  (will-unmount [_]
                (when-let [updatefn (om/get-state owner :updatecalback)]
                  (js/cancelAnimationFrame updatefn)))
  (render [_]
    (common/time-sexp :rendertime (
                             jscomponent/rapidstage
                             {:width width :height height :key "stage" :currentTime (om/get-state owner :faketime)}
                             (pixi/tilingsprite {:image (common/assetpath-for "bg_castle.png") :width width :height height :key "ack"})
                             (om/build common/controlpanel {:x 10 :y 10 :sprites sprites :spritecontrolchannel (om/get-state owner :spritecontrolchannel)})
                             ;;
                             ;; we use the #js form in the next sexp to prevent auto-conversion of clojurescript data structures into javascript objects;
                             ;; in particular, we want 'sprites' to get passed in as a "raw" clojurescript data structure without getting converted
                             ;;
                             (jscomponent/userchilddisplaycontainer #js {:customUpdater jscomponent/build-customchildren-iterator :customChildren @sprites :customComponent jsvortex/interpolatingSprite})
                             ;;(map jscomponent/interpolatingsprite sprites)
                             )))
  (display-name [_] "ExampleStage3"))

(defn getcomponentandstate []
  {:examplecomponent examplestage
   :exampleappstate appstate})
