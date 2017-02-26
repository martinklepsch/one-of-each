(ns ofe.app
  (:require [rum.core :as rum]
            [ofe.config :as config]
            [ofe.log :as log]
            [ofe.components :as c]
            [ofe.contentful :as content]
            [ofe.preferences :as pref]
            [ofe.core :as core]
            [bide.core :as r]
            [goog.object :as gobj]
            [goog.net.XhrIo :as xhr]))

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
                     (js->clj :keywordize-keys true))
           tracks (content/contentful->tracks items)]
       (reset! core/!state {:tracks tracks})
       (core/start-router!)
       (when (= :ofe/start (first (r/match core/router js/window.location.pathname)))
         (r/navigate! core/router :ofe/track {:track-slug (content/track-slug (first tracks))}))))))

(defn init []
  ;; (reset! !state (->> (js->clj js/window.ofe_static_appstate :keywordize-keys true)
  ;;                     (map content/map->Track)))
  (rum/mount (c/app core/!state) (. js/document (getElementById "container"))))
