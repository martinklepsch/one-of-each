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
                 [confetti/confetti         "0.1.5"      :scope "test"]
                 ;; static/server
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.473"]
                 [hiccup "1.0.5"]
                 [ring "1.5.1"]
                 [perun "0.4.2-SNAPSHOT"]
                 ;; client
                 [funcool/bide "1.4.0"]
                 [rum "0.10.8"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]]
 '[pandeiro.boot-http    :refer [serve]]
 '[confetti.boot-confetti :refer [sync-bucket]]
 '[deraen.boot-sass      :refer [sass]]
 '[io.perun              :as perun]
 '[ofe.static            :as ofe]
 '[ofe.contentful        :as content])

(deftask deps [])

(defn contentful-paths* [fs]
  (content/key-by :path (io.perun.meta/get-meta fs)))

(deftask contentful
  [r renderer RENDERER sym "page renderer (fully qualified symbol which resolves to a function)"]
  (perun/content-task {:task-name "contentful"
                       :render-form-fn (fn [data] `(ofe.static/render-track-page* ~data))
                       :paths-fn ofe/contentful-paths
                       :tracer :ofe.contentful/contentful
                       :pod perun/render-pod})
  #_(perun/render-task {:task-name "contentful"
                      :paths-fn ofe/contentful-paths
                      :renderer renderer
                      :tracer :ofe.contentful/contentful}))

(boot.pod/require-in @perun/render-pod 'ofe.static)
(boot.pod/require-in @perun/print-meta-pod 'ofe.static)
(task-options! contentful {:renderer 'ofe.static/render-track-page}
               perun/atom-feed {:out-dir ""
                                :extensions []
                                :base-url "https://one-of-each.xyz/"
                                :filterer (fn [page] (prn page) (.startsWith (:permalink page) "/t/"))
                                :site-title "A Music Blog"})


(defn my-passthru [inputs tracer global-meta]
  (io.perun/trace tracer
                  (for [[path {:keys [entry]}] inputs]
                    (do 
                      (prn entry)
                      (merge entry (io.perun.meta/path-meta path global-meta)
                             {:blba :blub})))))

(deftask build []
  (comp (speak)
        (with-pre-wrap fs
          (let [pages (ofe/contentful-meta nil)
                tmp   (tmp-dir!)]
            (doseq [p pages
                    :let [f (clojure.java.io/file tmp (:path p))]]
              (clojure.java.io/make-parents f)
              (.createNewFile f))
            (-> fs
                (add-resource tmp)
                (io.perun.meta/set-meta (ofe/contentful-meta nil))
                commit!)))
        ;; (cljs)
        ;; (sass)
        (perun/render-task {:task-name "contentful"
                            :paths-fn contentful-paths*
                            :renderer 'ofe.static/render-track-page
                            :tracer :ofe.contentful/contentful})

        ;; (contentful)
        )


        ;; TODO needs more work
        (perun/atom-feed))

(deftask run []
  (comp (serve :handler 'ofe.static/handler)
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

(deftask write-file-maps-edn []
  (with-pre-wrap fs
    (let [tmp (tmp-dir!)]
      (spit (clojure.java.io/file tmp "uploads.edn")
            (pr-str (mapv (fn [tf] {:s3-key (:path tf)
                                    :file (.getCanonicalPath (tmp-file tf))
                                    :metadata (when (.startsWith (:path tf) "t/")
                                                {:content-type "text/html"})})
                          (output-files fs))))
      (-> fs (add-resource tmp) commit!))))

(deftask deploy
  []
  (comp (production)
        (build)
        (sift :include #{#"^t/" #"js/app\.js" #"css/sass\.css" #"index\.html"})
        (write-file-maps-edn)
        (sync-bucket :bucket (System/getenv "S3_BUCKET_NAME")
                     :access-key (System/getenv "AWS_ACCESS_KEY")
                     :secret-key (System/getenv "AWS_SECRET_KEY")
                     :cloudfront-id (System/getenv "CLOUDFRONT_ID")
                     :file-maps-path "uploads.edn"
                     :prune true)))
