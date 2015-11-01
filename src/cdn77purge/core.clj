(ns cdn77purge.core
  (:require [org.httpkit.client :as http] ; http://www.http-kit.org/client.html
  	    [clojure.data.xml :as xml]
	    [clojure.java.io :as io]
            [cdn77purge.cdn77 :as cdn77]
            [diff-match-patch-clj.core :as dmp]
            [clojure.tools.cli :refer [parse-opts]]
            [mw.mwm :as mwm]
            [mw.mw1 :as mw1]
            )
  (:gen-class)
  )

(use 'diff-match-patch-clj.core)
(use 'clojure.tools.logging)


(def Cdn77
  "The parameters used to identify your CDN77 account
   https://client.cdn77.com/support/api/version/2.0/data#Prefetch"
  ;;(atom {:login "" :passwd "" :cdn_id "" :origin "" :cdn ""})
  (read-string (mw1/find-and-slurp "cdn77.config")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Manage state 
;;; (not used currently, we start from scratch everytime)

(comment 
(def State
  "All pages I know about and there last-modified information"
  (atom ()))

(mwm/defn2 update! [filename data]
  "data is the new data to associate with filename"
  (swap! State (fn [state] (assoc state filename data))))

(mwm/defn2 save-state []
  "Save state to disk"
  (spit "state.txt" (prn-str @State)))

(mwm/defn2 load-state []
  "Load state from disk"
  (let [state (read-string (slurp "state.txt"))]
    (reset! State state)))
)





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; In my case, switching between origin and master url is
;;; very simple

(mwm/defn2 make-origin [url] 
  "Make sure www is replaced by www2, unless we already have it"
  (if (.contains url "://www.")
    (clojure.string/replace url "://www." "://www2.")
    url))

(mwm/defn2 make-cdn [url] 
  "Make sure www2 is replaced by www, unless we already have it"
  (if (.contains url "://www2.")
    (clojure.string/replace url "://www2." "://www.")
    url))

(comment
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; identify stale files by using last-modified
;;; we could have used etag too, but that is not supported by CDN77
;;; (20150917)

(mwm/defn2 get-last-modified-origin [url]
  "Get the date from the origin"
  (let [headers (http/head (make-origin url))
        modified (-> @headers :headers :last-modified)]
    (list url modified)))

(mwm/defn2 get-last-modified-cdn [url]
  "Get the date from the origin"
  (let [headers (http/head (make-cdn url))
        modified (-> @headers :headers :last-modified)]
    (list url modified)))

(mwm/defn2 same-contents? [url]
  "return true of both CDN and origin has the same contents"
  (let [origin-last-modified (get-last-modified-origin url)
        cdn-last-modified (get-last-modified-cdn url)]
    (= origin-last-modified cdn-last-modified)))

)

;; I have to remove wordfence part of page, since it changes
;; ('//www2.spreadsheetconverter.com/?wordfence_logHuman=1&hid=7F2B40C98954833D308AB4F06377819B');/*]]>*
(mwm/defn2 remove-between [page start-text end-text]
  (let [start (.indexOf page start-text)
        end (.indexOf page end-text (+ start (.length start-text)))]
    (if (or (< start 0)(< end 0))
      page
      (str (.substring page 0 start) (.substring page end)))))

(mwm/defn2 keep-body [page]
  (.substring page (.indexOf page "<body")))

(mwm/defn2 remove-wordfence [page]
  (-> page
      (keep-body)
      (remove-between "'//www2.spreadsheetconverter.com/?wordfence_logHuman" "');/*]]>*")
      (remove-between "\"pjaxcontainer" "\"")
      (clojure.string/replace "gf_browser_chrome" "")
      (clojure.string/replace "gf_browser_unknown" "")
      (clojure.string/replace "gf_browser_gecko" "")
      (clojure.string/replace "<section\nclass=\"ipad-head\"><div\nclass=\"container\"> <a\nhref=\"#\"><img\nsrc=\"/wp-content/themes/ssc/img/btn-close.png\" width=\"130\" height=\"38\" alt=\"\"></a> <img\nsrc=\"/wp-content/themes/ssc/img/img-ipad.png\" width=\"430\" height=\"100\" alt=\"\"></div> </section> " "")
      ))

(mwm/defn2 same-contents? [url]
  "Check same contents by downloading page"
  (let [origin-contents-raw (:body @(http/get (make-origin url)))
        cdn-contents-raw (:body @(http/get (make-cdn url)))
        origin-contents (remove-wordfence origin-contents-raw)
        cdn-contents (remove-wordfence cdn-contents-raw)]
    (let [res (= origin-contents cdn-contents)]
      (if (not res)
        (do
          (println)
          (println "*************************************************")
          (println url)
          (doseq [one-diff
                  ;; only keep :ins and :del and diffs of more than 5 chars
                  (filter (fn [x] (and (not= (first x) :span) (> (count (nth x 2)) 5)))
                          (-> (diff origin-contents cdn-contents) cleanup! as-hiccup))]
            (let [op (nth one-diff 0)
                  data (nth one-diff 2)]
              (cond
                (= :ins op) (println "CDN: " data)
                (= :del op) (println " WP: " data)
                :else       (println "???: " one-diff))))
          (println "******")
          (spit "origin.log" (prn-str origin-contents))
          (spit "cdn.log" (prn-str cdn-contents))
          ))
      res)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Read sitemap.xml as XML and extract all loc:s

;; (mwm/defn2 remove-trailing-slash [url]
;;   (if (.endsWith url "/")
;;     (.substring url 0 (- (.length url) 1))
;;     url))

;;; todo: remove-backslash-slash should be completely unneccessary, remove it
(mwm/defn2 remove-backslash-slash [url]
  (-> url
      (clojure.string/replace "\\/" "/")
      ;; (remove-trailing-slash)
      ;; (str "?foo")
      ))

(mwm/defn2 get-sitemap []
  "Get the complete sitemap, parse it, and returns of the urls only"
  (let [sitemap-url (str (:origin Cdn77) "/sitemap.xml")
        sitemap-page (http/get sitemap-url)
        body (:body @sitemap-page)
        ;; xml/parse parse a file or stream, therefor the getBytes. Let us hope we do not run into UTF-8 problems
        sitemap (:content (xml/parse (java.io.ByteArrayInputStream. (.getBytes body "UTF-8"))))
        ]
    ;;; #clojure.data.xml.Element{:tag :url, :attrs {}, :content (#clojure.data.xml.Element{:tag :loc, :attrs {}, :content ("http://www.spreadsheetconverter.com/")} #clojure.data.xml.Element{:tag :lastmod, :attrs {}, :content ("2015-09-11T12:52:19+00:00")} #clojure.data.xml.Element{:tag :changefreq, :attrs {}, :content ("daily")} #clojure.data.xml.Element{:tag :priority, :attrs {}, :content ("1.0")})}
    (map (fn [x] (remove-backslash-slash (first (:content (first (:content x)))))) sitemap)
  )
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; first time, when do not have a state, we need to download all
;;; files from both origin and cdn. Once we have a state, we only download
;;; from origin and compare, and then force a refresh of cdn
(mwm/defn2 first-time-updates 
  "find all urls whose are not the same in origin and cdn"
  ;;([] (first-time-updates (take 50 (get-sitemap))))
  ([] (first-time-updates (get-sitemap)))

  ([sitemap] 

   ;; Which of the #(..%..) below is incorrect? Anyway (fn works
   ;; (let [compare-result (map #({:url % :same (same-contents? %)}) (take 10 sitemap))
   ;;       filtered (filter #((not (:same %))) compare-result)
   ;;       res (map #((:url %)) filtered)]

   ;;; pfilter implemented using pmap, filter and map
   (let [compare-result (pmap (fn [x] {:url x :same (same-contents? x)}) sitemap)
         filtered (filter (fn [x] (not (:same x))) compare-result)
         res (map (fn [x] (:url x)) filtered)]
     res)))

(def batch-size 20)

(mwm/defn2 cdn-apply
  "Ask cdn77 to apply a command like purge or prefetch.
  In order not to make the POST too big, 
  the number of urls per request is limited to batch-size"
  ([f stale-urls]
   (loop [stale-urls stale-urls]
     (if (not= stale-urls ())
       (let [first (take batch-size stale-urls)
             rest (nthrest stale-urls batch-size)]
         (f Cdn77 first)
         (recur rest))))))

(mwm/defn2 force-refresh 
  "Ask cdn77 to refresh all urls."
  ([]
   (let [stale-urls (first-time-updates)]
     (cdn-apply cdn77purge.cdn77/cdn77-purge stale-urls)
     ;; purge and prefetch are two separate queues at CDN77, so in order
     ;; to make sure purge and prefetch are done in the correct order, let us
     ;; wait a little
     (Thread/sleep 5000)
     (cdn-apply cdn77purge.cdn77/cdn77-prefetch stale-urls))))
   

;;; https://github.com/clojure/tools.cli
(def cli-options
  ;; An option with a required argument
  [["-a" "--all" "Purge the cache, incl cached .msi files"
    :id :all
    :default nil]
   ;; A non-idempotent option
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn -main
  "Find all files that differ in the between my origin site and the CDN, and request a prefetch for those"
  [& args]
  (let [options (:options (parse-opts args cli-options))]
    (println "Starting..." (:cdn Cdn77) (prn-str options) (.getParent (java.io.File. (mw1/this-jar? mw.mw1))))
    (if (:all options)
      (cdn77purge.cdn77/cdn77-purgeall Cdn77)
      (force-refresh))
    (shutdown-agents)
    (println "...Finished")
    ))


