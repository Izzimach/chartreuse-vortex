(ns chartreuse-vortex.server.core
  (require [ring.middleware.file :refer [wrap-file]]
           [ring.middleware.file-info :refer [wrap-file-info]]
      	   [compojure.core :refer [defroutes GET]]
           [compojure.route :refer [files resources]]))

;;
;; The react-pixi javascript files are stored in the react-pixi jar,
;; so we need the handler to serve those files properly. For any other
;; request the handler will rely on standard static file serving.
;;

(defroutes handler
  (resources "/react-pixi" {:root "react_pixi"})
  (files "/" {:root "."}))


