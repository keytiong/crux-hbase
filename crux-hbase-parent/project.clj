(defproject io.kosong.crux/crux-hbase-parent "0.1.0-SNAPSHOT"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :sum {:dir ".."}
  :plugins [[lein-modules "0.3.11"]]
  :packaging "pom"
  :profiles {:provided  {:dependencies [[org.clojure/clojure]]}
             :inherited {:javac-options ["-target" "1.8" "-source" "1.8"]}}
  :modules {:parent     nil
            :versions   {:crux                              "20.09-1.12.0-beta"
                         :hbase                             "2.2.5"
                         org.clojure/clojure                "1.10.1"
                         org.apache.hbase                   :hbase
                         juxt/crux-core                     :crux
                         io.kosong.crux/crux-hbase          :version
                         io.kosong.crux/crux-hbase-embedded :version}
            :subprocess nil})
