(defproject io.kosong.crux/crux-hbase "0.1.0-SNAPSHOT"
  :description "Crux HBase"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../crux-hbase-parent"}
  :dependencies [[juxt/crux-core "_"]
                 [org.apache.hbase/hbase-common _ :exclusions [net.minidev/json-smart]]
                 [org.apache.hbase/hbase-client _ :exclusions [net.minidev/json-smart]]])
