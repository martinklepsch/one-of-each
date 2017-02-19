(set-env!
 :source-paths    #{"sass" "src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-2"  :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.3"      :scope "test"]
                 [adzerk/boot-reload        "0.5.1"      :scope "test"]
                 [powerlaces/boot-cljs-devtools "0.2.0"  :scope "test"]
                 [binaryage/devtools        "0.9.1"      :scope "test"]
                 [pandeiro/boot-http        "0.7.6"      :scope "test"]
                 [com.cemerick/piggieback   "0.2.1"      :scope "test"]
                 [org.clojure/tools.nrepl   "0.2.12"     :scope "test"]
                 [weasel                    "0.7.0"      :scope "test"]
                 [deraen/boot-sass          "0.3.0"      :scope "test"]
                 [org.slf4j/slf4j-nop       "1.7.23"     :scope "test"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.473"]
                 [enlive "1.1.6"]
                 [perun "0.4.2-SNAPSHOT"]
                 [rum "0.10.8"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]]
 '[pandeiro.boot-http    :refer [serve]]
 '[deraen.boot-sass      :refer [sass]]
 '[io.perun              :as perun]
 '[ofe.static            :as ofe])

(deftask contentful
  [r renderer RENDERER sym "page renderer (fully qualified symbol which resolves to a function)"]
  (perun/render-task {:task-name "contentful"
                      :paths-fn ofe/contentful-paths
                      :renderer renderer
                      :tracer :ofe.contentful/contentful}))

(boot.pod/require-in @perun/render-pod 'ofe.static)
(task-options! contentful {:renderer 'ofe.static/render-track-page})

(deftask build []
  (comp (speak)
        (cljs)
        (sass)
        (contentful)))

(deftask run []
  (comp (serve)
        (watch)
        (cljs-devtools)
        (cljs-repl)
        (reload)
        (build)))


(deftask production []
  (task-options! cljs {:optimizations :advanced}
                 sass {:output-style :compressed})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none :source-map true}
                 reload {:on-jsload 'ofe.app/init})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))


