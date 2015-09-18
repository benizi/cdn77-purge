(ns cdn77purge.cdn77
  (:require [org.httpkit.client :as http])
  (:gen-class)
  )

(use 'clojure.tools.logging)

(def Todo
  "I need to batch the prefetch request, since I am only allowed to make 30 per 5 min"
  (atom ()))

(defn request-prefetch
  "Once the (go) command is send, post all outstanding requests"
  [url]
  (info (str "request-prefetch: " url))
  (swap! Todo (fn [todo] (conj todo (list :prefetch url)))))

(defn cdn77-prefetch [urls config]
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
      (info "Async HTTP POST: " status res))))

(defn go
  "Send all outstanding requests, currently, only prefetch is handled"
  []
  (info "go called")
  ;; TODO: 
  ;; # POST request - prefetching of multiple files at once
  ;; curl --data "cdn_id=xxx&login=name@domain.com&passwd=your_api_password&url[]=/images/hello.jpg&url[]=anotherimage.jpg" https://api.cdn77.com/v2.0/data/prefetch
  
  ;; # response
  ;; {"status":"ok","description":"Prefetch is accepted. It can take few minutes to upload all files to all datacentres.","url":["/images/hello.jpg","anotherimage.jpg"],"request_id":XXX}
  
  ())

