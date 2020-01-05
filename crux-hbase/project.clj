(defproject io.kosong.crux/crux-hbase "0.1.0-SNAPSHOT"
            :description "Crux HBase"
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[org.clojure/clojure]
                           [juxt/crux-core]
                           [org.apache.hbase/hbase-common]
                           [org.apache.hbase/hbase-client]]

  :java-source-paths ["src/java"]
  :source-paths ["src/clojure"])
