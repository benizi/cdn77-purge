(ns cdn77purge.core
  (:require [org.httpkit.client :as http]
  	    [clojure.data.xml :as xml]
	    [clojure.java.io :as io]
            )
  )

(def Cdn77
  "The parameters used to identify your CDN77 account
   https://client.cdn77.com/support/api/version/2.0/data#Prefetch"
  ;;(atom {:login "" :passwd "" :cdn_id "" :origin "" :cdn ""})
  (read-string (slurp "/home/mattias/cdn77.config")))


(def State
  "All pages I know about and there last-modified information"
  (atom ()))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Manage state

(defn update! [filename data]
  "data is the new data to associate with filename"
  (swap! State (fn [state] (assoc state filename data))))

(defn save-state []
  "Save state to disk"
  (spit "state.txt" (prn-str @State)))

(defn load-state []
  "Load state from disk"
  (let [state (read-string (slurp "state.txt"))]
    (reset! State state)))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; In my case, switching between origin and master url is
;;; very simple

(defn make-origin [url] 
  "Make sure www is replaced by www2, unless we already have it"
  (if (.contains url "://www.")
    (clojure.string/replace url "://www." "://www2.")
    url))

(defn make-cdn [url] 
  "Make sure www2 is replaced by www, unless we already have it"
  (if (.contains url "://www2.")
    (clojure.string/replace url "://www2." "://www.")
    url))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; identify stale files by using last-modified
;;; we could have used etag too, but that is not supported by CDN77
;;; (20150917)

(defn get-last-modified-origin [url]
  "Get the date from the origin"
  (let [headers (http/head (make-origin url))
        modified (-> @headers :headers :last-modified)]
    (list url modified)))

(defn get-last-modified-cdn [url]
  "Get the date from the origin"
  (let [headers (http/head (make-cdn url))
        modified (-> @headers :headers :last-modified)]
    (list url modified)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Read sitemap.xml as XML and extract all loc:s

(defn get-sitemap []
  "Get the complete sitemap, parse it, and returns of the urls only"
  (let [sitemap-url (str (:origin Cdn77) "/sitemap.xml")
        sitemap-page (http/get sitemap-url)
        body (:body @sitemap-page)
        ;; xml/parse parse a file or stream, therefor the getBytes. Let us hope we do not run into UTF-8 problems
        sitemap (:content (xml/parse (java.io.ByteArrayInputStream. (.getBytes body))))
        ]
    ;;; #clojure.data.xml.Element{:tag :url, :attrs {}, :content (#clojure.data.xml.Element{:tag :loc, :attrs {}, :content ("http://www.spreadsheetconverter.com/")} #clojure.data.xml.Element{:tag :lastmod, :attrs {}, :content ("2015-09-11T12:52:19+00:00")} #clojure.data.xml.Element{:tag :changefreq, :attrs {}, :content ("daily")} #clojure.data.xml.Element{:tag :priority, :attrs {}, :content ("1.0")})}
    (map (fn [x] (first (:content (first (:content x))))) sitemap)
  )
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; first time, when do not have a state, we need to download all
;;; files from both origin and cdn. Once we have a state, we only download
;;; from origin and compare, and then force a refresh of cdn
(defn first-time-updates 
  "find all urls whose are not the same in origin and cdn"
  ([] (first-time-updates (take 10 (get-sitemap))))

  ([sitemap] 
   (filter
    (fn [url]
      (let [origin-last-modified (get-last-modified-origin url)
            cdn-last-modified (get-last-modified-cdn url)]
        (not= origin-last-modified cdn-last-modified)))
    sitemap
    ))
  )

(defn force-refresh []
  (let [stale-urls (first-time-updates)]
    (doseq [url stale-urls] 
      (cdn77purge.cdn77/request-prefetch url))
    (cdn77purge.cdn77/go)))
