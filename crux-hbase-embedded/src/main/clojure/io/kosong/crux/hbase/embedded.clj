(ns io.kosong.crux.hbase.embedded
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [crux.system :as sys])
  (:import (org.apache.hadoop.hbase  HBaseConfiguration LocalHBaseCluster TableName NamespaceDescriptor NamespaceExistException)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.zookeeper.server ZooKeeperServer ServerCnxnFactory)
           (java.io Closeable)
           (org.apache.hadoop.metrics2.lib DefaultMetricsSystem)
           (org.apache.hadoop.hbase.client TableDescriptorBuilder ColumnFamilyDescriptorBuilder)))
;; Based on
;; https://github.com/apache/hbase/blob/master/hbase-server/src/main/java/org/apache/hadoop/hbase/master/HMasterCommandLine.java

(def default-zookeeper-port 2181)
(def default-zookeeper-tick-time 2000)

(defn- stop-hbase-cluster [^LocalHBaseCluster hbase-cluster]
  (DefaultMetricsSystem/shutdown)
  (.shutdown hbase-cluster)
  (.join hbase-cluster))

(defn- stop-zookeeper-server [^ZooKeeperServer zk-server]
  (let [conn-factory (.getServerCnxnFactory zk-server)]
    (.shutdown zk-server)
    (.shutdown conn-factory)))

(defrecord EmbeddedZookeeper [^ZooKeeperServer zk-server]
  Closeable
  (close [_]
    (stop-zookeeper-server zk-server)))

(defrecord EmbeddedHBase [hbase-cluster]
  Closeable
  (close [_]
    (stop-hbase-cluster hbase-cluster)))

(s/def ::zookeeper-data-dir ::sys/path)
(s/def ::zookeeper-port :crux.io/port)
(s/def ::zookeeper-tick-time int?)
(s/def ::hbase-config (s/map-of string? string?))

(s/def ::zookeeper-options (s/keys :req [::zookeeper-data-dir]
                                   :opt [::zookeeper-port
                                         ::zookeeper-tick-time]))

(s/def ::hbase-options (s/keys :opt [::zookeeper-port
                                     ::zookeeper-tick-time
                                     ::hbase-config]))

(defn- start-zookeeper-server ^ZooKeeperServer
  [{::keys [zookeeper-port zookeeper-tick-time zookeeper-data-dir]
    :or    {zookeeper-port      default-zookeeper-port
            zookeeper-tick-time default-zookeeper-tick-time}}]
  (let [max-conn 16
        data-dir (io/file zookeeper-data-dir)
        conn-factory (ServerCnxnFactory/createFactory zookeeper-port max-conn)
        server (ZooKeeperServer. data-dir
                                 data-dir
                                 zookeeper-tick-time)]
    (.startup conn-factory server)
    server))

(defn- hbase-conf [kvs]
  (let [conf (HBaseConfiguration/create)]
    (doseq [[^String k ^String v] kvs]
      (.set conf k v))
    conf))

(defn- start-hbase-cluster ^LocalHBaseCluster [{::keys [hbase-config]}]
  (DefaultMetricsSystem/setMiniClusterMode true)
  (let [hbase-cluster (doto (LocalHBaseCluster. (hbase-conf hbase-config))
                        (.startup))]
    hbase-cluster))

(defn start-embedded-hbase ^Closeable [options]
  (s/assert ::hbase-options options)
  (let [hbase-cluster (start-hbase-cluster options)]
    (->EmbeddedHBase hbase-cluster)))

(defn start-embedded-zookeeper ^Closeable [options]
  (s/assert ::zookeeper-options options)
  (let [zk-cluster (start-zookeeper-server options)]
    (->EmbeddedZookeeper zk-cluster)))

(defn ensure-namespace [conn ns]
  (let [ns-descriptor (-> ns
                          (NamespaceDescriptor/create)
                          (.build))]
    (with-open [admin (.getAdmin conn)]
      (try
        (.createNamespace admin ns-descriptor)
        (catch NamespaceExistException _)))))

(defn ensure-table [conn ^String ns ^String table ^String family]
  (ensure-namespace conn ns)
  (let [table-name       (TableName/valueOf (Bytes/toBytesBinary ns)
                                            (Bytes/toBytesBinary table))
        cf               (-> (ColumnFamilyDescriptorBuilder/newBuilder (Bytes/toBytesBinary family))
                             (.build))
        table-descriptor (-> (TableDescriptorBuilder/newBuilder table-name)
                             (.addColumnFamily cf)
                             (.build))]
    (with-open [admin (.getAdmin conn)]
      (when-not (.tableExists admin table-name)
        (.createTable admin table-descriptor)))))

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