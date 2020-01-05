(ns dev
  (:import (org.apache.hadoop.metrics2.lib DefaultMetricsFactory)
           (org.apache.hadoop.hbase HBaseConfiguration HConstants)
           (org.apache.hadoop.hbase.HBaseConfiguration)
           (org.apache.hadoop.hbase.zookeeper MiniZooKeeperCluster)
           (java.io File)
           (org.glassfish.hk2.api HK2Exception)))

(import org.apache.hadoop.hbase.HBaseConfiguration)

(import org.apache.hadoop.hbase.LocalHBaseCluster)
(import org.apache.hadoop.hbase.HConstants)
(import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster)
(import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem)


(def conf (doto (HBaseConfiguration/create)
            (.setInt HConstants/CLIENT_ZOOKEEPER_CLIENT_PORT HConstants/DEFAULT_ZOOKEEPER_CLIENT_PORT)
            (.setInt HConstants/ZOOKEEPER_TICK_TIME 2000)
            (.set HConstants/ZOOKEEPER_DATA_DIR "./data")))

(def zk-cluster (doto (MiniZooKeeperCluster. conf)
                  (.setDefaultClientPort (.getInt conf HConstants/CLIENT_ZOOKEEPER_CLIENT_PORT))
                  (.setTickTime (.getInt conf HConstants/ZOOKEEPER_TICK_TIME))))

(defn prep-zk [config]
  (merge config
         {HConstants/CLIENT_ZOOKEEPER_CLIENT_PORT HConstants/DEFAULT_ZOOKEEPER_CLIENT_PORT
          HConstants/ZOOKEEPER_TICK_TIME          2000
          HConstants/ZOOKEEPER_DATA_DIR           "./zk-data"}))

(defn init-zk [_ opts]
  (let [conf (doto (HBaseConfiguration/create)
               (.setInt HConstants/CLIENT_ZOOKEEPER_CLIENT_PORT
                        (opts HConstants/CLIENT_ZOOKEEPER_CLIENT_PORT))
               (.setInt HConstants/ZOOKEEPER_TICK_TIME
                        (opts HConstants/ZOOKEEPER_TICK_TIME)))
        port (opts HConstants/CLIENT_ZOOKEEPER_CLIENT_PORT)
        data-dir (opts HConstants/ZOOKEEPER_DATA_DIR)]
    (doto (MiniZooKeeperCluster. conf)
      (.setDefaultClientPort port)
      (.startup (File. data-dir)))))

(defn halt-zk! [zk]
  (.shutdown zk))

(defn prep-hbase [config]
  (merge config
         {HConstants/CLIENT_ZOOKEEPER_CLIENT_PORT HConstants/DEFAULT_ZOOKEEPER_CLIENT_PORT
          "hbase.masters" 1
          "hbase.master.start.timeout.localHBaseCluster" 300000
          "hbase.regionservers" 1}))

(defn init-hbase [_ opts]
  (let [conf (doto (HBaseConfiguration/create)
               )])
  )




