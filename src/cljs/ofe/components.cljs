(ns ofe.components
  (:require [rum.core :as rum]
            [bide.core :as r]
            [goog.events :as ev]
            [goog.ui.KeyboardShortcutHandler :as shortcut]
            [ofe.log :as log]
            [ofe.core :as core]
            [ofe.contentful :as content]
            [ofe.preferences :as pref])
  (:import [goog.ui KeyboardShortcutHandler]))

;; -------------------------------------------------------------------

;; (def slide-from-top
;;   {:from { :transform "translateY(-400px)" :opacity 0}
;;    :to   { :transform "" :opacity ""}})

;; (def slide-down
;;   {:from { :transform "" :opacity ""}
;;    :to   { :transform "translateY(900px)" :opacity 0}})

;; (defn create-element [react-comp opts & children]
;;   (apply js/React.createElement react-comp (clj->js opts) children))

;; (def flipmove* (partial create-element js/FlipMove))
;; (def transition-group (partial create-element js/React.addons.CSSTransitionGroup))
;; -----------------------------------------------------------------------------

;; (def keyboard-handler-mixin
;;   {:did-mount (fn [state]
;;                 (let [handler (KeyboardShortcutHandler. js/document)]
;;                   (.registerShortcut handler "next" "right")
;;                   (.registerShortcut handler "prev" "left")
;;                   (js/console.info "Shortcut handlers registered")
;;                   (ev/listen handler
;;                              shortcut/EventType.SHORTCUT_TRIGGERED
;;                              (fn [e]
;;                                (js/console.log (keyword (.-identifier e)))
;;                                #_(rf/dispatch [:shortcut (keyword (.-identifier e))])))
;;                   (assoc state ::keyboard-handler handler)))
;;    :will-unmount (fn [state]
;;                    (js/console.info "Unregistered shortcut handlers")
;;                    (.unregisterAll (::keyboard-handler state)))})

;; (rum/defc shortcuts < keyboard-handler-mixin
;;   []
;;   [:div#shortcut-handler])

;; -----------------------------------------------------------------------------

(def max-width 400)

(rum/defc logo []
  [:a.link.near-black
   {:href "/"}
   [:div.ma4
    {:style {:transform (str "rotate(" (rand-int 11) "deg)")
             :transition "transform 240ms"
             :transition-delay "500ms"}}
    [:h1.lh-solid "one" [:br] "of" [:br] "each"]]])

;; resize images:   var imageURL = 'https:' + asset.fields.file.url + '?w=200';
(rum/defc spotify-player
  [spotify-track-uri]
  {:pre [(string? spotify-track-uri)]}
  [:iframe.db {:src (str "https://embed.spotify.com/?uri=" spotify-track-uri)
               :width max-width :height 80 :frameBorder 0 :allowTransparency true}])

(rum/defc soundcloud-player
  [soundcloud-id]
  {:pre [(string? soundcloud-id)]}
  [:iframe.db {:width max-width :height 110 :scrolling "no" :frameBorder "no"
               :src (str "https://w.soundcloud.com/player/?url=https%3A//api.soundcloud.com/tracks/" soundcloud-id "&amp;color=111&amp;auto_play=false&amp;hide_related=false&amp;show_comments=true&amp;show_user=true&amp;show_reposts=false")}])

(def humanize-platform
  {:spotify "Spotify"
   :soundcloud "Soundcloud"})

(rum/defc player-button
  [on-click platform track-info]
  (assert (humanize-platform platform))
  [:div
   [:button.btn-reset.b
    {:on-click on-click}
    (humanize-platform platform)
    (when (and (= :soundcloud platform)
               (not (:soundcloud-official? track-info)))
      [:sup.normal "✝"])]])

(rum/defcs track-players < rum/reactive (rum/local nil ::show-player) (rum/local nil ::hide-pref-prompt?)
  {:key-fn (fn [track-info] (:title track-info))}
  [s track-info]
  (let [platform-pref @pref/always-load-platform
        shown-player  (rum/react (::show-player s))
        show-player?  (when-not (false? shown-player)
                        (or shown-player platform-pref))]
    (js/console.log show-player?)
    [:div
     [:div.big-box-shadow
      [:img.db
       {:width max-width
        :height max-width
        :src (str "https:" (:cover-art track-info) "?w=" max-width)}]
      (when show-player?
        (case show-player?
          :spotify    (spotify-player (:spotify-uri track-info))
          :soundcloud (soundcloud-player (:soundcloud-id track-info))))]

     (when-not show-player?
       [:div.cf
        (when (:spotify-uri track-info)
          [:div.fl.w-50.tc.pv3 (player-button #(reset! (::show-player s) :spotify) :spotify track-info)])
        (when (:soundcloud-id track-info)
          [:div.fl.w-50.tc.pv3 (player-button #(reset! (::show-player s) :soundcloud) :soundcloud track-info)])])

     (cond
       (and show-player?
            (nil? platform-pref)
            (not (rum/react (::hide-pref-prompt? s))))
       [:div.pa3.tc.f6
        [:span.db "Always load the " (humanize-platform shown-player) " player? "
         [:button.btn-reset.link {:on-click #(do (pref/reset pref/always-load-platform shown-player)
                                                 (reset! (::hide-pref-prompt? s) true))}"yes"]
         "/"
         [:button.btn-reset.link {:on-click #(reset! (::hide-pref-prompt? s) true)} "no, thanks"]]]

       show-player?
       [:div.pa3.tr.f6
        [:button.btn-reset.link.silver
         {:on-click #(do (reset! (::hide-pref-prompt? s) nil)
                         (reset! (::show-player s) false))}
         "× close player"]])]))


(rum/defc track [track-info]
  [:div
   [:div.cf
    [:div.fl-ns.w-100.w-auto-ns
     (track-players track-info)]
    [:div.fl-ns.pa4
     [:h2.f2.mb2.mt0 (:title track-info)]
     [:p.lh-copy
      [:span.f6.mid-gray.db [:span.gray " by "] (:artist track-info)]
      [:span.f6.mid-gray.db [:span.gray " on "] (:album track-info) " (" (:year track-info) ")"]]]]])

(rum/defc site-meta []
  [:div.tr.f6.gray.pr2.lh-copy
   [:span.db.mb2 [:sup.normal "✝"] "inoffical"]
   [:span.db "curated by " [:a.link.dark-gray {:href "https://twitter.com/martinklepsch"} "@martinklepsch"]]
   [:span.db "have " [:a.link.dark-gray {:href "mailto:martinklepsch+oneofeach@googlemail.com"} "something to say?"]]
   [:button.btn-reset.f6.pa0 {:on-click #(pref/clear pref/always-load-platform)} "clear preferences"]])

(rum/defc app < rum/reactive
  [state-atom]
  (let [tracks  (:tracks (rum/react state-atom))
        idx     (:current (rum/react state-atom))
        current (when (seq tracks) (get tracks idx))
        prev    (when (seq tracks) (get tracks (dec idx)))
        next    (when (seq tracks) (get tracks (inc idx)))]
    (js/console.log "tracks" tracks)
    (js/console.log "current track" idx)
    [:div.cf
     [:div.fixed-ns
      (logo)
      [:div.ph4.pv3
       [:a.f6.mb3.link.pv3
        {:style    (when-not prev {:pointerEvents "none"})
         :class    (if prev "near-black" "silver")
         :href     (when prev (str "/t/" (content/track-slug prev)))
         :on-click #(do (.preventDefault %)
                        (r/navigate! core/router :ofe/track {:track-slug (content/track-slug prev)}))}
        "← prev"]
       [:span " · "]
       [:a.f6.mb3.link.pv3
        {:style    (when-not next {:pointerEvents "none"})
         :class    (if next "near-black" "silver")
         :href     (when next (str "/t/" (content/track-slug next)))
         :on-click #(do (.preventDefault %)
                        (r/navigate! core/router :ofe/track {:track-slug (content/track-slug next)}))}
        "next →"]]]
     (when current
       [:div.mt6-ns.ml7-ns.fl-ns
        (track current)])
     [:div.fixed-ns.right-0.bottom-0.pa3.bg-white
      (site-meta)]
     #_(shortcuts)]))
