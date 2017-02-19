(ns ofe.static
  (:require [rum.core :as rum]
            [net.cgrand.enlive-html :as html]
            [clojure.data.json :as json]
            [ofe.components :as c]
            [ofe.contentful :as content]))

(html/deftemplate main-template "index.html"
  [track-data container-contents]
  [:#container]  (html/html-content container-contents)
  [:#track-data] (html/content (str "var track = " (json/write-str track-data))))

(defn contentful-paths [_]
  (->> (content/get-tracks!)
       (content/contentful->tracks)
       (content/key-by #(str "t/" (:id %) ".html"))))

(defn render-track-page [track]
  (apply str (main-template track (rum/render-html (c/app (atom [track]))))))

(comment
  (contentful-paths 0)

  (boot.pod/require-in @perun/render-pod 'ofe.static)

  (boot.core/boot (contentful :renderer 'ofe.static/render-track-page))

  )
