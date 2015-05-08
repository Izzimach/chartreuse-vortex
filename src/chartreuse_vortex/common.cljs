;;
;; Constants and functions used by all the examples.
;;

(ns chartreuse-vortex.common
  (:require-macros [cljs.core.async.macros :refer [go]])

  (:require [om.core :as om :include-macros true]
            [om-tools.core :as omtools :refer-macros [defcomponentk] :include-macros true]
            [omreactpixi.abbrev :as pixi]
            [cljs.core.async :as async :refer [>! <! put! chan]]
            [schema.core :as schema]
            [clojure.string :as string]))

(defn assetpath-for [name] (str "../assets/" name))

(def *spriteimage* (assetpath-for "WardenIcon.png"))
(def *spritesize* 128)
(def *gravity* 500)
(def *defaulttextstyle* {:font "14 KenVector_Future_Thin"})
(def *defaulttexttint* 16r00010101)
(def *halfscale* (js/PIXI.Point. 0.5 0.5))
(def *spritecenter* (js/PIXI.Point. 0.5 0.5))

(defn- scale [x y]
  (if (or (zero? x) (zero? y))
    1
    (js/Math.abs x)))

(defn float=
  "Compare floats to see if they're equal, taking into account the issues of comparing floating point numbers.
  Taken from _The_Clojure_Cookbook_"
  ([x y] (float= x y 0.00001))
  ([x y epsilon] (<= (js/Math.abs (- x y))
                     (* (scale x y) epsilon))))


(defn shrinkbyspritesize [{:keys [width height]}]
  [(- width *spritesize*) (- height *spritesize*)])

(defcomponentk clickable-sprite [[:data x y image clickchannel clickdata] owner]
  (display-name [_] "ClickableSprite")
  (render [_] (let [clickfeed #(put! clickchannel clickdata)]
                (pixi/sprite {:x x :y y :interactive true :image image :anchor *spritecenter* :click clickfeed}))))


(defcomponentk spritecountlabel [[:data x y spritecount] owner]
  (display-name [_] "SpriteCountLabel")
  (render [_]
          (pixi/bitmaptext {:x x :y y :style *defaulttextstyle* :tint *defaulttexttint* :text (str "Sprite count: " spritecount)})))

;;
;; when you invoke 'time-sexp' the results get dumped in here and can be displayed
;; with the timinglabels component
;;

(defonce timingdata (atom {:rendertime 0 :updatetime 0 :basic 0}))

(def *panelscale* (js/PIXI.Point. 2 1))

(defcomponentk timinglabels [[:data x y render-time update-time] owner]
  (display-name [_] "TimingLabels")
  (render [_ ]
    (pixi/displayobjectcontainer
      {:x x :y y}
      (pixi/bitmaptext {:x 0 :y 0 :key "rendertimelabel" :style *defaulttextstyle* :tint *defaulttexttint*
                        :text (str "Render " (.toFixed render-time 2) " ms")})
      (pixi/bitmaptext {:x 0 :y 10 :key "updatetimelabel" :style *defaulttextstyle* :tint *defaulttexttint*
                        :text (str "Update " (.toFixed update-time 2) " ms")}))))

;;
;; just displays the various start/stop controls. if any of them are clicked, it pushes the name of the clicked
;; icon into the click channel
;;
(defcomponentk timing-icons [[:data x y recording? clickchannel] owner]
  (display-name [_] "TimingIcons")
  (render [_]
    (let [startstopsprite (if recording?
                            (assetpath-for "stop.png")
                            (assetpath-for "target.png"))]
      (pixi/displayobjectcontainer
        {:x x :y y}
        (om/build clickable-sprite {:x 20 :y 20 :key "startstop" :image startstopsprite :clickchannel clickchannel :clickdata "startstop"})
        (om/build clickable-sprite {:x 60 :y 20 :key "cleardata" :image (assetpath-for "trashcan.png") :clickchannel clickchannel :clickdata "cleardata"})
        (om/build clickable-sprite {:x 100 :y 20 :key "download" :image (assetpath-for "import.png") :clickchannel clickchannel :clickdata "download"})))))

;;
;; handles clicking of the timing icons to start and stop recording of timing data and downloading it
;;
(defcomponentk timing-controls [[:data x y spritecount] owner]
  (display-name [_] "TimingControls")
  (init-state [_] {:recording? false :timingdata [] :clickchannel (chan 1) :rendertime 0 :updatetime 0})
  (did-mount [_]
    (let [clickchannel (om/get-state owner :clickchannel)
          startstoprecording (fn []
                               (let [recording?        (om/get-state owner :recording?)
                                     newrecordingstate (not recording?)]
                                 (om/set-state! owner :recording? newrecordingstate)))
          appendtimingdata (fn []
                             (let [{:keys [rendertime updatetime timingdata]} (om/get-state owner)
                                   newtimingdata                              (conj timingdata [spritecount rendertime updatetime])]
                               (om/set-state! owner :timingdata newtimingdata)))
          updatefn (fn updatecallback [_]
                     (let [{:keys [rendertime updatetime]} @timingdata]
                       (om/set-state! owner :rendertime rendertime)
                       (om/set-state! owner :updatetime updatetime)
                       (if (om/get-state owner :recording?)
                         (appendtimingdata))))]
      ;; process clicks on the various icons
      (go (loop [clickdata (<! clickchannel)]
            (.warn js/console clickdata)
            (cond
              (= clickdata "startstop") (startstoprecording)
              (= clickdata "cleardata") (om/set-state! owner :timingdata [])
              (= clickdata "download") nil
              :else (.warn js/console (str "Invalid data came through click channel:" clickdata)))
            (recur (<! clickchannel))))
      ;;
      ;; this auto-updates the data values at a set interval by pulling them out of the 'timingdata' atom
      ;;
      (om/set-state! owner :updatecallback (js/setInterval updatefn 500))))
  (will-unmount [_]
    ;; need to clean up the auto-updating
    (when-let [updatefn (om/get-state owner :updatecalback)]
      (js/clearInterval updatefn)))
  (render-state [_ state]
    (let [commonprops                                (select-keys state [:recording? :clickchannel])
          {:keys [rendertime updatetime timingdata]} state
          timingsamplecount                          (count timingdata)]
      (pixi/displayobjectcontainer
        {:x x :y y}
        (om/build timinglabels (assoc commonprops :x 0 :y 0 :render-time rendertime :update-time updatetime :spritecount spritecount))
        (om/build timing-icons (assoc commonprops :x 0 :y 24))
        (pixi/bitmaptext {:x 0 :y 65 :style *defaulttextstyle* :tint *defaulttexttint* :text (str timingsamplecount " samples collected")})))))

(defcomponentk controlpanel [[:data x y spritecount] owner]
  (display-name [_] "TimingLabels")
  (render [_]
    (pixi/displayobjectcontainer
      {:x x :y y}
      (pixi/sprite {:image (assetpath-for "blue_panel.png") :scale *panelscale*})
      (om/build spritecountlabel {:x 10 :y 5 :spritecount spritecount})
      (om/build timing-controls {:x 10 :y 16 :spritecount spritecount}))))
