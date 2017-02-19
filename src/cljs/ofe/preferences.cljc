(ns ofe.preferences)

(defprotocol IUserPreference
  (initialize [this])
  (reset [this value])
  (clear [this]))

#?(:cljs
   (defrecord LocalStoragePreference [name ^:mutable value serialize de-serialize default]
     IUserPreference
     (initialize [this]
       (vreset! value (de-serialize (.getItem (.-localStorage js/window) (str name))))
       this)
     (reset [this new-value]
       (vreset! value new-value)
       (.setItem (.-localStorage js/window) (str name) (serialize new-value)))
     (clear [this]
       (vreset! value nil)
       (.removeItem (.-localStorage js/window) (str name)))
     IDeref
     (-deref [this]
       (deref value))))

(def always-load-platform
  #?(:cljs (initialize (->LocalStoragePreference ::always-load-platform? (volatile! nil) name keyword nil))
     :clj  (atom nil)))

;; (def always-load-spotify?
;;   (initialize (->LocalStoragePreference ::always-load-spotify? (volatile! nil) name keyword nil)))

;; (def always-load-soundcloud?
;;   (initialize (->LocalStoragePreference ::always-load-soundcloud? (volatile! false) name keyword false)))
