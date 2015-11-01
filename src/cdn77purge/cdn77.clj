(ns cdn77purge.cdn77
  (:require [org.httpkit.client :as http]
            [mw.mwm :as mwm]
            [mw.mw1 :as mw1]
            )
  (:gen-class)
  )

(use 'clojure.tools.logging)

(defn cdn77-urls [url msg config urls]
  (if (not= urls ())
    (let [;;the one without the www2
          site (:cdn config)              
          ;;remove host part from urls, only keep local path
          no-site-urls (map #(clojure.string/replace % site "") urls) 
          options {:form-params {:cdn_id (:cdn_id config)
                                 :login (:login config)
                                 :passwd (:passwd config)
                                 "url[]" no-site-urls}}
          res @(http/post url options)
          {:keys [status error]} res]
      (println (str msg (vec no-site-urls)))
      (if error (error "Failed, exception is " error res)))))

;;; https://client.cdn77.com/support/api/version/2.0/data#Prefetch
(mwm/defn2 cdn77-prefetch [config urls]
  (cdn77-urls "https://api.cdn77.com/v2.0/data/prefetch"
              "prefetched "
              config urls))

;;; https://client.cdn77.com/support/api/version/2.0/data#Purge
;;; https://api.cdn77.com/v2.0/data/purge
(mwm/defn2 cdn77-purge [config urls]
  (cdn77-urls "https://api.cdn77.com/v2.0/data/purge"
              "purged "
              config urls))

(defn cdn77-purgeall [config]
  (let [options {:form-params {:cdn_id (:cdn_id config)
                               :login (:login config)
                               :passwd (:passwd config)}}
        res @(http/post "https://api.cdn77.com/v2.0/data/purge-all" options)
        {:keys [status error]} res]
    (info "purgeall, PLEASE rerun in about 10 minutes to fill caches. ")
    (if error (error "Failed, exception is " error res))))

