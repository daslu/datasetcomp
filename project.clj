(defproject datasetcomp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bedatadriven public repo"
    			        {:url "https://nexus.bedatadriven.com/content/groups/public/"}]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [spork "0.2.1.1-SNAPSHOT" :exclusions [org.clojure/core.async]]
                 [org.clojure/core.async "0.4.490"]
                 [techascent/tech.ml-base "3.3"]
                 [tech.tablesaw/tablesaw-core "LATEST"]
                 [com.clojure-goes-fast/clj-memory-meter "0.1.2"]
                 [org.renjin/renjin-script-engine "0.9.2718"]])
