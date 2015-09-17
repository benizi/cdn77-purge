(defproject cdn77purge "0.1.0-SNAPSHOT"
  :description "Loop over all pages on your site and prefetch the changed ones"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/data.xml "0.0.8"]
                 ]
  :main ^:skip-aot cdn77purge.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  )
