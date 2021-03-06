(defproject org.clojars.haussman/chartreuse-vortex "0.1.0-SNAPSHOT"
  :description "Sample app using om-react-pixi" 
  :url "https://github.com/Izzimach/chartreuse-vortex"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3165"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojars.haussman/om-react-pixi "0.5.1"]
                 [prismatic/schema "0.4.2"]
                 [prismatic/om-tools "0.3.11"]
                 [ring "1.3.2"]
                 [compojure "1.3.3"]
                 [environ "1.0.0"]
                 [figwheel "0.2.6"]]

  :source-paths ["src"]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-ring "0.8.10"]
            [lein-environ "1.0.0"]
            [com.cemerick/clojurescript.test "0.3.3"]]

  ;; dammit leiningen, clean up my stuff
  :clean-targets ^{:protect false} ["resources/public/out"]
  
  :profiles {
             :dev {
                   :figwheel {
                              :http-server-root "public"
                              :server-port 8081
                              :ring-handler chartreuse-vortex.server.core/handler
                              :nrepl-port 7888
                              }
                   :plugins [[lein-figwheel "0.2.6"]]}}
  
  :ring { :handler chartreuse-vortex.server.core/handler :port 8081 }

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src" "dev_src"]
                        :compiler {
                                   :output-to "resources/public/chartreuse-vortex.js"
                                   ;; custom server handler also serves from 'target'
                                   :output-dir "resources/public/out"
                                   :asset-path "out"
                                   :libs ["resources/public/js"]
                                   :main chartreuse-vortex.dev
                                   :optimizations :none
                                   :source-map true}}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {
                                   :output-to "resources/public/chartreuse-vortex.min.js"
                                   :main chartreuse-vortex.core
                                   :libs ["resources/public/js"]
                                   :optimizations :advanced
                                   :closure-warnings {:externs-validation :off
                                                      :non-standard-jsdoc :off}}}]})

