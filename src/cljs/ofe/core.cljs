(ns ofe.core
  (:require [bide.core :as r]
            [ofe.contentful :as content]
            [goog.events])
  (:import goog.history.EventType
           goog.history.Html5History))

(defonce !state (atom {:tracks []
                       :current 0}))


;; bide --

(defn slug->idx [track-slug]
  (.indexOf (to-array (mapv :id (:tracks @!state))) (content/track-slug->id track-slug)))

(def router
  (r/router [["/" :ofe/start]
             ["/t/:track-slug" :ofe/track]]))

(defn on-navigate
  "A function which will be called on each route change."
  [name {:keys [track-slug] :as params} query]
  (js/console.log "Navigating to" name track-slug)
  (swap! !state assoc :current (slug->idx track-slug)))

(defn start-router! []
  (r/start! router {:default :ofe/start
                    :on-navigate on-navigate
                    :html5? true}))
