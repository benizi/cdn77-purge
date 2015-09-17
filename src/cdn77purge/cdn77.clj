(ns cdn77purge.cdn77
  (:require [org.httpkit.client :as http]))

(def Todo
  "I need to batch the prefetch request, since I am only allowed to make 30 per 5 min"
  (atom ()))

(defn prefetch
  "Once the (go) command is send, post all outstanding requests"
  [url]
  (swap! Todo (fn [todo] (conj todo (list :prefetch url)))))

(defn go
  "Send all outstanding requests, currently, only prefetch is handled"
  []
  ;; TODO: 
  ;; # POST request - prefetching of multiple files at once
  ;; curl --data "cdn_id=xxx&login=name@domain.com&passwd=your_api_password&url[]=/images/hello.jpg&url[]=anotherimage.jpg" https://api.cdn77.com/v2.0/data/prefetch
  
  ;; # response
  ;; {"status":"ok","description":"Prefetch is accepted. It can take few minutes to upload all files to all datacentres.","url":["/images/hello.jpg","anotherimage.jpg"],"request_id":XXX}
  
  ())

