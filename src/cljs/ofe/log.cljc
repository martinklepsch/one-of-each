(ns ofe.log
  (:refer-clojure :exclude [time])
  (:require #?(:clj  [clojure.tools.logging :as log]
               :cljs [goog.log :as glog]))
  #?(:cljs (:require-macros [ofe.log]))
  #?(:cljs (:import goog.debug.Console)))

#?(:cljs
   (def logger
     ;; TODO the used namespace should be dynamic
     (glog/getLogger "app")))

#?(:cljs
   (def levels {:severe goog.debug.Logger.Level.SEVERE
                :warning goog.debug.Logger.Level.WARNING
                :info goog.debug.Logger.Level.INFO
                :config goog.debug.Logger.Level.CONFIG
                :fine goog.debug.Logger.Level.FINE
                :finer goog.debug.Logger.Level.FINER
                :finest goog.debug.Logger.Level.FINEST}))

#?(:cljs
   (defn log-to-console! []
     (.setCapturing (goog.debug.Console.) true)))

#?(:cljs
   (defn set-level! [level]
     (.setLevel logger (get levels level (:info levels)))))

(defn fmt [msgs]
  (apply str (interpose " " (map pr-str msgs))))

#?(:clj
   (defmacro logfn [log-fn]
     (println (-> &env :ns :name symbol))
     `(partial ~log-fn (glog/getLogger ~(-> &env :ns :name str)))))

#?(:cljs ((ofe.log/logfn glog/info) "hello"))

;; TODO custom formatters don't work with goog.log
;; because it logs strings and no objects
(defn info [& s]
  (let [msg (fmt s)]
    #?(:clj  (log/info msg)
       :cljs (js/console.info s))))

(defn debug [& s]
  (let [msg (fmt s)]
    #?(:clj  (log/debug msg)
       :cljs (js/console.log msg))))

(defn error [& s]
  (let [msg (fmt s)]
    #?(:clj (log/error msg)
       :cljs (glog/error logger msg))))
