(ns crux.hbase
  (:require [crux.kv.hbase]
            [crux.node :as n]
            [crux.standalone])
  (:import (org.apache.hadoop.hbase.client ConnectionFactory)
           (org.apache.hadoop.hbase HBaseConfiguration TableName)
           (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.hbase.util Bytes)))


(defn- start-hbase-client [_ _]
  (let [c (doto (Configuration.)
            (.set "hbase.rootdir" "./hbase"))]
    (-> (HBaseConfiguration/create c)
        (ConnectionFactory/createConnection))))

(defn- start-index-kv [{:keys [crux.hbase/hbase-connection]}
                       {:keys [crux.hbase/namespace
                               crux.hbase/index-table
                               crux.hbase/index-family
                               crux.hbase/index-qualifier]
                        :as   options}]
  (let [table-name (TableName/valueOf ^bytes (Bytes/toBytesBinary namespace)
                                      ^bytes (Bytes/toBytesBinary index-table))
        table      (.getTable hbase-connection table-name)
        family (Bytes/toBytesBinary index-family)
        qualifier (Bytes/toBytesBinary index-qualifier)
        kv (crux.kv.hbase/->HBaseKvStore hbase-connection table family qualifier)]
    kv))

(def ^:private hbase-options
  {::family
   {:doc              "HBase column family name"
    :default          "crux"
    :crux.config/type :crux.config/string}
   ::namespace
   {:doc              "HBase namespace"
    :default          "default"
    :crux.config/type :crux.config/string}
   ::qualifier
   {:doc              "HBase column qualifier name"
    :default          "val"
    :crux.config/type :crux.config/string}})


(def index-kv-module
  {:start-fn start-index-kv
   :deps     [:crux.hbase/hbase-connection]
   :args     (merge hbase-options
                    {:crux.hbase/index-table
                     {:doc              "Index table name"
                      :default          "index"
                      :crux.config/type :crux.config/string}})})

(def topology
  (merge
    n/base-topology
    {:crux.hbase/hbase-connection {:start-fn start-hbase-client}
     :n/kv-store          crux.hbase/index-kv-module
     }))
