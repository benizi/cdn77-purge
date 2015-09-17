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
