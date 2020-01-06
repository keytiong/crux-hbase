(ns user
  (:require [crux.hbase.embedded]
            [crux.hbase]
            [crux.api :as crux])
  (:import (crux.api ICruxAPI)
           (org.apache.hadoop.hbase.client ConnectionFactory TableDescriptorBuilder ColumnFamilyDescriptorBuilder)
           (org.apache.hadoop.hbase HBaseConfiguration NamespaceDescriptor TableName)
           (org.apache.hadoop.hbase.util Bytes)))

(def embedded-hbase-conf
  {:crux.hbase.embedded/zookeeper-data-dir "zk-data-dir"
   :crux.hbase.embedded/hbase-data-dir     "hbase-data-dir"
   :crux.hbase.embedded/hbase-config {"hbase.tmp.dir" "./hbase-tmp"
                                      "hbase.rootdir" "./hbase"}})

(def embedded-hbase nil)

(defn start-embedded-hbase
  ([]
   (start-embedded-hbase embedded-hbase-conf))
  ([options]
   (alter-var-root #'embedded-hbase
                   (fn [_]
                     (crux.hbase.embedded/start-embedded-hbase options)))))

(defn stop-embedded-hbase []
  (when embedded-hbase
    (.close embedded-hbase)
    (alter-var-root #'embedded-hbase (constantly nil))))

(defn open-connection []
  (-> (HBaseConfiguration/create)
      (ConnectionFactory/createConnection)))

(defn create-namespace [conn ns]
  (let [ns-descriptor (-> ns
                          (NamespaceDescriptor/create)
                          (.build))
        admin         (.getAdmin conn)]
    (.createNamespace admin ns-descriptor)))

(defn create-table [conn ^String ns ^String table ^String family]
  (let [table-name       (TableName/valueOf (Bytes/toBytesBinary ns)
                                            (Bytes/toBytesBinary table))
        cf               (-> (ColumnFamilyDescriptorBuilder/newBuilder (Bytes/toBytesBinary family))
                             (.build))
        table-descriptor (-> (TableDescriptorBuilder/newBuilder table-name)
                             (.addColumnFamily cf)
                             (.build))
        admin            (.getAdmin conn)]
    (.createTable admin table-descriptor)))

(defn ^ICruxAPI start-node []
  (crux/start-node {:crux.node/topology   :crux.hbase/topology
                    :crux.hbase/namespace "basilia"}))

(defn ^ICruxAPI start-standalone-node []
  (crux/start-node
    {:crux.node/topology                 :crux.standalone/topology
     :crux.node/kv-store                 "crux.kv.memdb/kv"
     :crux.standalone/event-log-dir      "data/eventlog-1"
     :crux.kv/db-dir                     "data/db-dir"
     :crux.standalone/event-log-kv-store "crux.kv.memdb/kv"}))

(defn ^ICruxAPI start-hbase-node []
  (crux/start-node
    {:crux.node/topology      :crux.hbase/topology
     :crux.hbase/index-table  "index"
     :crux.hbase/namespace    "basilia"
     :crux.hbase/tx-log-table "txlog"
     :crux.hbase/object-table "object"
     :crux.kv/check-and-store-index-version true}))