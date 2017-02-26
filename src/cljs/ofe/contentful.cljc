(ns ofe.contentful
  (:require [clojure.string :as string])
  #?(:clj (:require [clojure.data.json :as json])))

(def site-base-uri "https://one-of-each.xyz/")
(def contentful-base-uri "https://cdn.contentful.com/")
(def contentful-space-id "lv8xkcuq2e10")
(def contentful-access-token "717fea63337a2460cc98f539021cf9c43d062dd305864553c56729edc58cbe92")

#?(:clj
   (defn get-tracks! []
     (-> (str contentful-base-uri
              "/spaces/"
              contentful-space-id
              "/entries?access_token="
              contentful-access-token
              "&content_type=" "track")
         (slurp)
         (json/read-str :key-fn keyword))))

(defn track-slug [track-info]
  (-> (str (string/lower-case (str (:title track-info) "-" (:artist track-info))) "-" (:id track-info))
      (string/replace #"\s+" "-")))

(defn track-uri [track-info]
  (str "t/" (track-slug track-info)))

(defn track-slug->id [track-slug]
  (last (string/split track-slug #"-")))

(defrecord Track [id title artist album year cover-art
                  spotify-uri soundcloud-id soundcloud-official?
                  posted-at ;should be part of a "parent" record but to lazy rn
                  ])

(defn key-by [kfn xs]
  (reduce (fn [m i]
            (if (get m (kfn i))
              (throw (ex-info "duplicate key" {:key (kfn i)}))
              (assoc m (kfn i) i)))
          {}
          xs))

(defn contentful->tracks [{:keys [items includes] :as data}]
  (-> (fn [i]
        (map->Track {:id (-> i :sys :id)
                     :posted-at (-> i :sys :createdAt)
                     :title (-> i :fields :title)
                     :artist (-> i :fields :artist)
                     :album (-> i :fields :album)
                     :year (-> i :fields :year)

                     :spotify-uri (-> i :fields :spotifyUri)
                     :soundcloud-id (-> i :fields :soundcloudId)
                     :soundcloud-official? (-> i :fields :soundcloudOfficial)

                     :cover-art (let [asset-id (-> i :fields :coverArt :sys :id)
                                      assets   (key-by (comp :id :sys) (:Asset includes))]
                                  (get-in assets [asset-id :fields :file :url]))}))
      (mapv items)))



(comment

  (contentful->tracks (get-tracks!))

  )
