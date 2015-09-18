(ns cdn77purge.cdn77
  (:require [org.httpkit.client :as http]
            )
  (:gen-class)
  )

(use 'clojure.tools.logging)

;;; https://client.cdn77.com/support/api/version/2.0/data#Prefetch
(defn cdn77-prefetch [urls config]
  (if (not= urls ())
    (let [site (:cdn config)              ;the one without the www2
          no-site-urls (map #(clojure.string/replace % site "") urls)
          options {:form-params {:cdn_id (:cdn_id config)
                                 :login (:login config)
                                 :passwd (:passwd config)
                                 "purge_first" "1"
                                 "url[]" no-site-urls}}
          res @(http/post "https://api.cdn77.com/v2.0/data/prefetch" options)
          {:keys [status error]} res]
      (info site)
      (info no-site-urls)
      (if error (error "Failed, exception is " error res)))))

