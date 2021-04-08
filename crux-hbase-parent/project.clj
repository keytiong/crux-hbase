(defproject io.kosong.crux/crux-hbase-parent "0.3.0-SNAPSHOT"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :sum {:dir ".."}
  :plugins [[lein-modules "0.3.11"]]
  :packaging "pom"
  :profiles {:provided  {:dependencies [[org.clojure/clojure]]}
             :inherited {:javac-options ["--release" "8"]}}
  :modules {:parent     nil
            :versions   {:crux                              "21.04-1.16.0-beta"
                         :hbase                             "2.3.3"
                         org.clojure/clojure                "1.10.1"
                         org.apache.hbase                   :hbase
                         juxt/crux-core                     :crux
                         juxt/crux-kafka-embedded           :crux
                         io.kosong.crux/crux-hbase          :version
                         io.kosong.crux/crux-hbase-embedded :version}
            :subprocess nil})
