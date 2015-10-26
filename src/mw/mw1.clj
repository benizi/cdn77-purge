(ns mw.mw1
  (:require
   [mw.mwm :as mwm]
   ;;   [org.httpkit.client :as http] ; http://www.http-kit.org/client.html
   [clojure.xml :as c-xml]
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
;;   [clojure.tools.cli :refer [parse-opts]]
   )
  (:gen-class)
  )

(require '[clojure.pprint :as pp]
         '[clojure.walk :as walk])

;; (use 'clojure.tools.logging)

(mwm/defn2 find-and-slurp
  "Search and slurp for file in this dir, and all parents until found. Throw exception if not found. Max 50 levels"
  ([filename] (find-and-slurp filename 50 ""))
  ([filename level prefix]
   ;; in order to allow :post
   ;; (loop [filename filename level level prefix prefix]
   (if (< level 0) 
     (throw (Exception. (str "Not found: " filename)))
     (if (.exists (clojure.java.io/as-file (str prefix filename)))
       (slurp (str prefix filename))
       (recur filename (- level 1) (str "../" prefix))))))


(mwm/defn2 of-xml-string
  "Convert a UTF-8 string into an clojure structure"
  [str]
  (xml/parse (java.io.ByteArrayInputStream. (.getBytes str "UTF-8"))))

;; (def x1 (mw1/of-xml-string (:body @(http/get (:url Config)))))
;; (xml-keep-tag-content x1)

(mwm/defn2 xml-keep-tag-content
  "Keep recursive map and only keep :tag and :content as key and value.
   The resulting structure looks like json."
  ([pxml]
   ;; (if (:tag pxml)
   ;; we want nil since (if
   (if (:tag? pxml)
     (xml-keep-tag-content (:tag pxml)(:content pxml))
     pxml))
  ([tag content]
   {tag
    (if (seq? content)
      (let [res (for [pxml content] (xml-keep-tag-content pxml))]
        ;; assume keys are unique and store into map on each level
        ;; if you replace {} by [], instead we can handle duplicates on each level
        (if (> (count res) 1) (into {} res) (first res)))
      content)}))


