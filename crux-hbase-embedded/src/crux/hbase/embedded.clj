(ns crux.hbase.embedded
  (:require [clojure.java.io :as io]
            [crux.io]
            [clojure.spec.alpha :as s])
  (:import (org.apache.hadoop.hbase HBaseConfiguration LocalHBaseCluster)
           (org.apache.hadoop.hbase.zookeeper MiniZooKeeperCluster)
           (java.io Closeable)
           (org.apache.hadoop.hbase.regionserver HRegionServer)
           (org.apache.hadoop.hbase.master HMasterCommandLine$LocalHMaster)
           (org.apache.hadoop.metrics2.lib DefaultMetricsSystem)))
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

(defn- hbase-conf [options]
  (let [conf (HBaseConfiguration/create)]
    (doseq [[^String k ^String v] (::hbase-config options)]
      (.set conf k v))
    conf))

(defn- start-hbase-cluster ^LocalHBaseCluster [zk-cluster {m ::hbase-config}]
  (let [conf          (hbase-conf m)
        masters       1
        regionservers 1
        hbase-cluster (LocalHBaseCluster. conf masters regionservers HMasterCommandLine$LocalHMaster HRegionServer)]
    (DefaultMetricsSystem/setMiniClusterMode true)
    (doto hbase-cluster
      (.startup))))

(defn start-embedded-hbase ^Closeable [options]
  (s/assert ::options options)
  (let [zk-cluster    (start-zookeeper-cluster options)
        hbase-cluster (start-hbase-cluster zk-cluster options)]
    (->EmbeddedHBase zk-cluster hbase-cluster)))