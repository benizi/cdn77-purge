(ns cdn77purge.cdn77
  (:require [org.httpkit.client :as http])
  (:gen-class)
  )

(use 'clojure.tools.logging)

(defn cdn77-prefetch [urls config]
  (if (not= urls ())
    (let [site (:cdn config)              ;the one without the www2
          no-site-urls (map #(clojure.string/replace % site "") urls)
          options {:form-params {:cdn_id (:cdn_id config)
                                 :login (:login config)
                                 :passwd (:passwd config)
                                 ;; todo: need to handle more urls
                                 "url[]" no-site-urls}}
          res @(http/post "https://api.cdn77.com/v2.0/data/prefetch" options)
          {:keys [status error]} res]
      (info site)
      (info no-site-urls)
      (if error
        (error "Failed, exception is " error res)
        (info "Async HTTP POST: " status res)))))
