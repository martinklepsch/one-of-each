(ns ofe.app
  (:require [rum.core :as rum]
            [ofe.config :as config]
            [ofe.log :as log]
            [ofe.components :as c]
            [ofe.contentful :as content]
            [ofe.preferences :as pref]
            [goog.object :as gobj]
            [goog.net.XhrIo :as xhr]))

(defonce !state (atom {}))
(def contentful-base-uri "https://cdn.contentful.com/")

;; /spaces/{space_id}/entries?access_token={access_token}&content_type={content_type}
(defonce x
  (xhr/send
   (str contentful-base-uri
        "/spaces/"
        config/contentful-space-id
        "/entries?access_token="
        config/contentful-access-token
        "&content_type=" "track")
   (fn [ev]
     (let [items (-> (.. ev -target getResponseJson)
                     (js->clj :keywordize-keys true))]
       (reset! !state (content/contentful->tracks items))))))

(defn init []
  (reset! !state [(content/map->Track (js->clj js/window.track :keywordize-keys true))])
  (rum/mount (c/app !state) (. js/document (getElementById "container"))))
