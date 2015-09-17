(ns cdn77purge.core
  (:require [org.httpkit.client :as http])
  (:require [clojure.data.xml :as xml])
  )

(def Cdn77
  "The parameters used to identify your CDN77 account
   https://client.cdn77.com/support/api/version/2.0/data#Prefetch"
  ;;(atom {:login "" :passwd "" :cdn_id "" :origin "" :cdn ""})
  (read-string (slurp "c:/data/cdn77.config")))


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

(defn save []
  "Save state to disk"
  (spit "state.txt" (prn-str @State)))

(defn load []
  "Load state from disk"
  (let [state (read-string (slurp "state.txt"))]
    (reset! State state)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Read sitemap.xml as XML and extract all loc:s

(defn get-sitemap []
  "Get the complete sitemap, and parse it"
  (let [sitemap-url (str (:origin Cdn77) "/sitemap.xml")
        sitemap-page (http/get sitemap-url)
        body (:body @sitemap-page)
        ;; sitemap (clojure.data.xml/parse body)
        ]
    body
  )
)
