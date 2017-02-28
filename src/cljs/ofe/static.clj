(ns ofe.static
  (:require [hiccup.page :as hp]
            [clojure.tools.logging :as log]
            [pandeiro.boot-http.impl]
            [ofe.contentful :as content]))

(defn contentful-paths [_]
  (->> (content/get-tracks!)
       (content/contentful->tracks)
       (map #(assoc % :uuid (:id %)))
       (content/key-by content/track-uri)))

(defn contentful-meta [_]
  (->> (content/get-tracks!)
       (content/contentful->tracks)
       (map #(assoc % :uuid (:id %)))
       (map #(assoc % :path (content/track-uri %)))
       (map #(into {} %))))

(defn render-track-page [track]
  (hp/html5
   {}
   (let [title (str "one of each: " (:title track) " by " (:artist track))
         img-width 600]
     [:head {:prefix "og: http://ogp.me/ns# fb: http://ogp.me/ns/fb# music: http://ogp.me/ns/music#"}
      [:title title]
      [:link {:href "/css/sass.css" :rel "stylesheet" :type "text/css" :media "screen"}]

      [:meta {:content "380101619041086" :property "fb:app_id"}]
      [:meta {:content "music.song" :property "og:type"}]
      [:meta {:content (str content/site-base-uri (content/track-uri track)) :property "og:url"}]
      [:meta {:content title :property "og:title"}]
      [:meta {:content (str "https:" (:cover-art track) "?w=" img-width) :property "og:image"}]
      [:meta {:content img-width :property "og:image:width"}]
      [:meta {:content img-width :property "og:image:height"}]
      ;; These need to point to pages with the respective type
      ;; [:meta {:content (:album track) :property "music:album"}]
      ;; [:meta {:content (:artist track) :property "music:musician"}]
      [:meta {:content (:year track) :property "music:release_date"}]

      [:meta {:content "summary_large_image", :name "twitter:card"}]
      [:meta {:content "@oneofeach", :name "twitter:site"}]
      [:meta {:content "@martinklepsch" :name "twitter:creator"}]
      [:meta {:content title :name "twitter:title"}]
      [:meta {:content title :name "twitter:description"}]
      [:meta {:content (str "https:" (:cover-art track) "?w=" 600) :name "twitter:image"}]

      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]])
   [:body.system-sans-serif.dark-gray
    [:div#container]
    [:script {:type "text/javascript" :src "/js/app.js"}]]))

(defn render-track-page* [track]
  (prn track)
  (assoc track :rendererd (render-track-page track)))

;; The default resources handler can't determine the content type for
;; track pages properly so we do it manually here

(defn wrap-content-type [handler]
  (fn [req]
    (let [res (handler req)]
      (if (.startsWith (:uri req) "/t/")
        (assoc-in res [:headers "Content-Type"] "text/html")
        res))))

(def handler
  (-> (pandeiro.boot-http.impl/resources-handler {})
      (wrap-content-type)))

(comment
  (contentful-paths nil)

  (boot.pod/require-in @perun/render-pod 'ofe.static)

  (boot.core/boot (contentful :renderer 'ofe.static/render-track-page))

  (require 'io.perun.core)

  (io.perun.core/filename "hello/world.html")

  )
