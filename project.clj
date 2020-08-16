(defproject io.kosong.crux/crux "0.1.0-SNAPSHOT"
  :description "Crux KV implementation with HBase"
  :url "https://github.com/keytiong/crux-hbase"
  :license {:name "The MIT License"
            :url  "http://opensource.org/licenses/MIT"}
  :plugins [[lein-modules "0.3.11"]]

  :dependencies [[org.clojure/clojure]
                 [io.kosong.crux/crux-hbase :version]
                 [io.kosong.crux/crux-hbase-embedded :version]
                 [io.kosong.crux/crux-hbase-test :version]]

  :profiles {:dev       {:source-paths ["dev"]
                         :dependencies [[integrant "0.8.0"]
                                        [integrant/repl "0.3.1"]
                                        [nrepl "0.6.0"]]}}

  :modules {:dirs ["crux-hbase" "crux-hbase-embedded" "crux-hbase-test"]
            :versions {:hbase              "2.2.5"
                       :crux               "20.08-1.10.1-beta"
                       org.apache.hbase    :hbase
                       org.clojure/clojure "1.10.1"
                       juxt/crux-core      :crux}
            :subprocess nil}

  :aliases {"clean" ["do" ["modules" "clean"]]
            "build" ["do" ["modules" "install"]]})

(defn enable-unsecured-http-wagon []
  (require 'cemerick.pomegranate.aether)
  (cemerick.pomegranate.aether/register-wagon-factory!
    "http" #(org.apache.maven.wagon.providers.http.HttpWagon.)))

(enable-unsecured-http-wagon)

