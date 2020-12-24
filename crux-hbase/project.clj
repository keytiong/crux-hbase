(defproject io.kosong.crux/crux-hbase "0.2.0-SNAPSHOT"
  :description "Crux HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :scm {:dir ".."}
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../crux-hbase-parent"}
  :source-paths ^:replace ["src/main/clojure"]
  :java-source-paths ^:replace ["src/main/java"]
  :dependencies [[juxt/crux-core _]
                 [org.apache.hbase/hbase-client _ :exclusions [commons-logging
                                                               net.minidev/json-smart
                                                               org.slf4j/slf4j-log4j12]]
                 ;; dependency conflict resolution
                 [com.google.guava/guava "16.0.1"]
                 [org.slf4j/slf4j-api "1.7.30"]])
