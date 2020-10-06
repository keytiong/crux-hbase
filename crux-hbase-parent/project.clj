(defproject io.kosong.crux/crux-hbase-parent "0.1.0-SNAPSHOT"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :plugins [[lein-modules "0.3.11"]]
  :packaging "pom"
  :profiles {:provided  {:dependencies [[org.clojure/clojure _]]}
             :inherited {:test-paths        ["src/test/clojure"]
                         :java-source-paths ["src/main/java"]
                         :source-paths      ["src/main/clojure"]}}
  :modules {:parent     nil
            :versions   {org.clojure/clojure                "1.10.1"
                         org.apache.hbase                   "2.2.5"
                         juxt/crux-core                     "20.09-1.12.0-beta"
                         io.kosong.crux/crux-hbase          :version
                         io.kosong.crux/crux-hbase-embedded :version}
            :subprocess nil})
