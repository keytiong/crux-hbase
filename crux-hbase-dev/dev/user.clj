(ns user
  (:require [crux.hbase.embedded]
            [crux.hbase]
            [crux.api :as crux]
            [integrant.core :as i]
            [integrant.repl.state :refer [system]]
            [integrant.repl :as ir :refer [clear go suspend resume halt reset reset-all]])
  (:import (crux.api ICruxAPI)
           (org.apache.hadoop.hbase.client ConnectionFactory TableDescriptorBuilder ColumnFamilyDescriptorBuilder)
           (org.apache.hadoop.hbase HBaseConfiguration NamespaceDescriptor TableName NamespaceExistException)
           (org.apache.hadoop.hbase.util Bytes)))

(defn open-connection []
  (-> (HBaseConfiguration/create)
      (ConnectionFactory/createConnection)))

(defn ensure-namespace [conn ns]
  (let [ns-descriptor (-> ns
                          (NamespaceDescriptor/create)
                          (.build))
        admin         (.getAdmin conn)]
    (try
      (.createNamespace admin ns-descriptor)
      (catch NamespaceExistException e))))

(defn ensure-table [conn ^String ns ^String table ^String family]
  (let [table-name       (TableName/valueOf (Bytes/toBytesBinary ns)
                                            (Bytes/toBytesBinary table))
        cf               (-> (ColumnFamilyDescriptorBuilder/newBuilder (Bytes/toBytesBinary family))
                             (.build))
        table-descriptor (-> (TableDescriptorBuilder/newBuilder table-name)
                             (.addColumnFamily cf)
                             (.build))
        admin            (.getAdmin conn)]
    (when-not (.tableExists admin table-name)
      (.createTable admin table-descriptor))))

(defn- prep-hbase [ns table family]
  (let [conn (open-connection)]
    (try
      (do
        (ensure-namespace conn ns)
        (ensure-table conn ns table family))
      (finally (.close conn)))))

(defmethod i/init-key :node [_ node-opts]
  (crux/start-node node-opts))

(defmethod i/halt-key! :node [_ ^ICruxAPI node]
  (.close node))

(defmethod i/init-key :embedded-hbase [_ hbase-opts]
  (let [hbase (crux.hbase.embedded/start-embedded-hbase hbase-opts)]
    (prep-hbase "default" "index" "crux")
    hbase))

(defmethod i/halt-key! :embedded-hbase [_ hbase]
  (.close hbase))

(def config
  {:embedded-hbase {:crux.hbase.embedded/zookeeper-data-dir "zk-data-dir"
                    :crux.hbase.embedded/hbase-data-dir     "hbase-data-dir"
                    :crux.hbase.embedded/hbase-config       {"hbase.tmp.dir" "./hbase-tmp"
                                                             "hbase.rootdir" "./hbase"}}

   :node           {:crux.node/topology                    ['crux.standalone/topology
                                                            'crux.kv.hbase/kv-store]
                    :crux.kv.hbase/index-table             "index"
                    :crux.kv.hbase/namespace               "default"
                    :crux.kv.hbase/index-family            "crux"
                    :crux.kv.hbase/index-qualifier         "val"
                    :crux.kv/check-and-store-index-version true}})

(ir/set-prep! (fn [] config))

(defn crux-node []
  (:node system))