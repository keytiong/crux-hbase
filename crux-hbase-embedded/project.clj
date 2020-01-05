(defproject io.kosong.crux/crux-hbase-embedded "0.1.0-SNAPSHOT"
  :description "Embedded HBase or development and testing"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[org.clojure/clojure]
                 [juxt/crux-core]
                 [org.apache.hbase/hbase-server]])
