(defproject io.kosong.crux/crux-hbase-embedded "0.1.0-SNAPSHOT"
  :description "Embedded HBase for development and testing"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../crux-hbase-parent"}
  :dependencies [[org.apache.hbase/hbase-common _ :exclusions [net.minidev/json-smart]]
                 [org.apache.hbase/hbase-client _ :exclusions [net.minidev/json-smart]]
                 [org.apache.hbase/hbase-server _ :exclusions [net.minidev/json-smart
                                                               org.glassfish/javax.el]]])

