(ns user
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as ctn]))

(ctn/disable-reload!)
(apply ctn/set-refresh-dirs [(io/file "." "crux-hbase" "src" "main" "clojure")
                             (io/file "." "crux-hbase-embedded" "src" "main" "clojure")
                             (io/file "." "crux-hbase-test" "src" "main" "clojure")
                             (io/file "." "crux-hbase-test" "src" "test" "clojure")])

(defn dev []
  (require 'dev)
  (in-ns 'dev))