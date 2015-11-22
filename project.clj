;;; TIP: lein ancient to find updates

(defproject cdn77purge "0.6.3"
  :description "Loop over all pages on your site and prefetch the changed ones"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.19"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/tools.logging "0.3.1"]
                 ;; (refresh) seems to work better if I switch to user first: (in-ns 'user)
                 ;; user=> (require '[clojure.tools.namespace.repl :refer [refresh]])
                 ;; user=> (refresh)
                 [org.clojure/tools.namespace "0.2.11"]
                 ;; https://github.com/scusack/diff-match-patch-clj
                 [diff-match-patch-clj "1.0.0-SNAPSHOT"]
                 [org.clojure/tools.cli "0.3.3"]
                 ]
  :main ^:skip-aot cdn77purge.core
  :aot [mw.mw1 mw.mwm]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  )
