(ns io.kosong.crux.hbase.embedded
  (:require [clojure.java.io :as io]
            [crux.io]
            [clojure.spec.alpha :as s])
  (:import (org.apache.hadoop.hbase HBaseConfiguration LocalHBaseCluster TableName NamespaceDescriptor NamespaceExistException)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.hbase.zookeeper MiniZooKeeperCluster)
           (java.io Closeable)
           (org.apache.hadoop.hbase.regionserver HRegionServer)
           (org.apache.hadoop.hbase.master HMasterCommandLine$LocalHMaster)
           (org.apache.hadoop.metrics2.lib DefaultMetricsSystem)
           (org.apache.hadoop.hbase.client TableDescriptorBuilder ColumnFamilyDescriptorBuilder)))
;; Based on
;; https://github.com/apache/hbase/blob/master/hbase-server/src/main/java/org/apache/hadoop/hbase/master/HMasterCommandLine.java

(def default-zookeeper-port 2181)
(def default-zookeeper-tick-time 2000)

(defn- stop-hbase-cluster [^LocalHBaseCluster hbase-cluster]
  (.shutdown hbase-cluster)
  (.join hbase-cluster))

(defn- stop-zookeeper-cluster [^MiniZooKeeperCluster zk-cluster]
  (.shutdown zk-cluster))

(defrecord EmbeddedZookeeper [^MiniZooKeeperCluster zk-cluster]
  Closeable
  (close [_]
    (stop-zookeeper-cluster zk-cluster)))

(defrecord EmbeddedHBase [zk-cluster hbase-cluster]
  Closeable
  (close [_]
    (stop-hbase-cluster hbase-cluster)
    (stop-zookeeper-cluster zk-cluster)))

(s/def ::zookeeper-data-dir string?)
(s/def ::zookeeper-port :crux.io/port)
(s/def ::zookeeper-tick-time int?)
(s/def ::hbase-data-dir string?)
(s/def ::hbase-config (s/map-of string? string?))

(s/def ::options (s/keys :req [::zookeeper-data-dir
                               ::hbase-data-dir]
                         :opt [::zookeeper-port
                               ::zookeeper-tick-time
                               ::hbase-config]))

(defn- start-zookeeper-cluster ^MiniZooKeeperCluster
  [{::keys [zookeeper-port zookeeper-tick-time zookeeper-data-dir]
    :or    {zookeeper-port      default-zookeeper-port
            zookeeper-tick-time default-zookeeper-tick-time}}]
  (doto (MiniZooKeeperCluster. (HBaseConfiguration/create))
    (.setDefaultClientPort zookeeper-port)
    (.setTickTime zookeeper-tick-time)
    (.startup (io/file zookeeper-data-dir))))

(defn- hbase-conf [kvs]
  (let [conf (HBaseConfiguration/create)]
    (doseq [[^String k ^String v] kvs]
      (.set conf k v))
    conf))

(defn- start-hbase-cluster ^LocalHBaseCluster [{kvs ::hbase-config}]
  (DefaultMetricsSystem/setMiniClusterMode true)
  (doto (LocalHBaseCluster. (hbase-conf kvs)
                            1
                            1
                            HMasterCommandLine$LocalHMaster
                            HRegionServer)
    (.startup)))

(defn start-embedded-hbase ^Closeable [options]
  (s/assert ::options options)
  (let [zk-cluster    (start-zookeeper-cluster options)
        hbase-cluster (start-hbase-cluster options)]
    (->EmbeddedHBase zk-cluster hbase-cluster)))

(defn ensure-namespace [conn ns]
  (let [ns-descriptor (-> ns
                          (NamespaceDescriptor/create)
                          (.build))
        admin         (.getAdmin conn)]
    (try
      (.createNamespace admin ns-descriptor)
      (catch NamespaceExistException e))))

(defn ensure-table [conn ^String ns ^String table ^String family]
  (ensure-namespace conn ns)
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

(defn create-table [conn ns table family]
  (ensure-namespace conn ns)
  (ensure-table conn ns table family))

(defn delete-table [conn ^String ns ^String table]
  (let [admin (.getAdmin conn)
        table-name (TableName/valueOf (Bytes/toBytesBinary ns)
                                      (Bytes/toBytesBinary table))]
    (when (.tableExists admin table-name)
      (.disableTable admin table-name)
      (.deleteTable admin table-name))))