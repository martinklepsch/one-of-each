(ns ofe.components
  (:require [rum.core :as rum]
            [ofe.log :as log]
            [ofe.preferences :as pref]))

(def max-width 400)

(rum/defc logo []
  [:div.ma4
   {:style {:transform (str "rotate(" 7 #_(rand-int 11) "deg)")
            :translate "transform 200ms"}}
   [:h1.lh-solid "one" [:br] "of" [:br] "each"]])

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
   [:span.db.b.mb2
    {:on-click on-click}
    (humanize-platform platform)
    (when (and (= :soundcloud platform)
               (not (:soundcloud-official? track-info)))
      [:sup.normal "✝"])]])

(rum/defcs track-players < rum/reactive (rum/local nil ::show-player) (rum/local nil ::hide-pref-prompt?)
  [s track-info]
  (let [platform-pref @pref/always-load-platform
        shown-player  (or platform-pref (rum/react (::show-player s)))]
    [:div
     (case shown-player
       :spotify    (spotify-player (:spotify-uri track-info))
       :soundcloud (soundcloud-player (:soundcloud-id track-info))
       [:div
        {:style {:max-width max-width}}
        (when (:spotify-uri track-info)
          [:div.fl.w-50.tc.pv3 (player-button #(reset! (::show-player s) :spotify) :spotify track-info)])
        (when (:soundcloud-id track-info)
          [:div.fl.w-50.tc.pv3 (player-button #(reset! (::show-player s) :soundcloud) :soundcloud track-info)])])

     [:div.pa3.tr.f6
      {:style {:max-width max-width}}
      (cond
        (and shown-player (nil? platform-pref)
             (not (rum/react (::hide-pref-prompt? s))))
        [:span.db "Should we always load the " (humanize-platform shown-player) " player? "
         [:a.link {:on-click #(do (pref/reset pref/always-load-platform shown-player)
                                  (reset! (::hide-pref-prompt? s) true))}"yes"]
         " / "
         [:a.link {:on-click #(reset! (::hide-pref-prompt? s) true)} "no, thanks"]]

        (or shown-player platform-pref)
        [:a.link {:on-click #(do (reset! (::hide-pref-prompt? s) nil)
                                 (reset! (::show-player s) nil))}
         "× close player"]


        )]]))

(rum/defc track [track-info]
  (log/info track-info)
  [:div.mb6
   [:div.cf
    [:img.fl-ns.w-100.w-auto-ns {:src (str "https:" (:cover-art track-info) "?w=" max-width)}]
    [:div.fl-ns.pa3
     [:h2.mb2.mt0 (:title track-info)]
     [:span.f6.mid-gray.db [:span.gray " by "] (:artist track-info)]
     [:span.f6.mid-gray.db [:span.gray " on "] (:album track-info) " (" (:year track-info) ")"]]]
   (track-players track-info)])

(rum/defc site-meta []
  [:div.tr.f6.gray.pr2.lh-copy
   [:span.db.mb2 [:sup.normal "✝"] "inoffical"]
   [:span.db "Curated by " [:a.link.dark-gray {:href "https://twitter.com/martinklepsch"} "@martinklepsch"]]
   [:span.db "Have something to say?"]])

(rum/defc app < rum/reactive
  [state-atom]
  [:div.cf
   [:div.fixed-ns
    (logo)
    [:div.pa4
     [:a.f6 {:on-click #(pref/clear pref/always-load-platform)}
      "clear preferences"]]]
   [:div.mt6-ns.ml7-ns.fl-ns
    (track (first (rum/react state-atom)))
    #_(mapv (fn [t] (track t))
          (take 2 (rum/react state-atom)))]
   [:div.fixed-ns.right-0.bottom-0.pa3.bg-white
    (site-meta)]])
