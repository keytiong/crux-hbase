(defproject io.kosong.crux/crux-hbase "0.1.0-SNAPSHOT"
            :description "Crux HBase"
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[org.clojure/clojure]
                           [juxt/crux-core]
                           [org.apache.hbase/hbase-common :exclusions [net.minidev/json-smart]]
                           [org.apache.hbase/hbase-client :exclusions [net.minidev/json-smart]]]

  :java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]

  :profiles {:dev {:test-paths ["src/test/clojure"]
                   :dependencies [[org.clojure/test.check "0.10.0"]
                                  [io.kosong.crux/crux-hbase-embedded :version]]}})
