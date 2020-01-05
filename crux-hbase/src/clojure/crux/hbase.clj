(ns crux.hbase
  (:require [crux.kv.hbase]
            [crux.moberg :as moberg]
            [crux.node :as n]
            [crux.tx.polling :as p]
            [crux.lru :as lru]
            [crux.object-store :as object-store]
            [crux.standalone])
  (:import (org.apache.hadoop.hbase.client ConnectionFactory)
           (org.apache.hadoop.hbase HBaseConfiguration)
           (org.apache.hadoop.conf Configuration)))


(defn- start-hbase-client [_ _]
  (let [c (doto (Configuration.)
            (.set "hbase.rootdir" "./hbase"))]
    (-> (HBaseConfiguration/create c)
        (ConnectionFactory/createConnection))))

(defn- start-index-kv [{:keys [crux.hbase/hbase-connection]}
                       {:keys [crux.hbase/namespace
                               crux.hbase/index-table
                               crux.hbase/family
                               crux.hbase/qualifier]
                        :as   options}]
  (let [kv (crux.kv.hbase/->HBaseKvStore hbase-connection namespace index-table family qualifier)]
    (crux.lru/start-kv-store kv options)))

(defn- start-tx-log-kv [{:keys [crux.hbase/hbase-connection]}
                        {:keys [crux.hbase/namespace
                                crux.hbase/tx-log-table
                                crux.hbase/family
                                crux.hbase/qualifier]
                         :as   options}]
  (let [kv (crux.kv.hbase/->HBaseKvStore hbase-connection namespace tx-log-table family qualifier)]
    (crux.lru/start-kv-store kv options)))

(defn- start-object-kv [{:keys [crux.hbase/hbase-connection]}
                        {:keys [crux.hbase/namespace
                                crux.hbase/object-table
                                crux.hbase/family
                                crux.hbase/qualifier]
                         :as   options}]
  (let [kv (crux.kv.hbase/->HBaseKvStore hbase-connection namespace object-table family qualifier)]
    (crux.lru/start-kv-store kv options)))

(defn- start-tx-log [{:keys [crux.hbase/tx-log-kv]}
                     options]
  (moberg/->MobergTxLog tx-log-kv))

(defn- start-object-store [{:keys [crux.hbase/object-kv]}
                           {:keys [crux.object-store/doc-cache-size]}]
  (let [cache        (lru/new-cache doc-cache-size)
        object-store (object-store/->KvObjectStore object-kv)]
    (object-store/->CachedObjectStore cache object-store)))

(defn- start-tx-log-consumer [{:keys [crux.hbase/tx-log-kv crux.node/indexer]} _]
  (when tx-log-kv
    (p/start-event-log-consumer indexer
                                (moberg/map->MobergEventLogConsumer {:event-log-kv tx-log-kv
                                                                     :batch-size   100}))))

(def ^:private lru-options
  {:crux.lru/query-cache-size
   {:doc              "Query Cache Size"
    :default          lru/default-query-cache-size
    :crux.config/type :crux.config/nat-int}})

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
   :args     (merge lru-options
                    hbase-options
                    {:crux.hbase/index-table
                     {:doc              "Index table name"
                      :default          "index"
                      :crux.config/type :crux.config/string}})})

(def tx-log-kv-module
  {:start-fn start-tx-log-kv
   :deps     [:crux.hbase/hbase-connection]
   :args     (merge hbase-options
                    {:crux.hbase/tx-log-table
                     {:doc              "Transaction log table name"
                      :default          "txlog"
                      :crux.config/type :crux.config/string}})})

(def object-kv-module
  {:start-fn start-object-kv
   :deps     [:crux.hbase/hbase-connection]
   :args     (merge hbase-options
                    {:crux.hbase/object-table
                     {:doc              "Object table name"
                      :default          "object"
                      :crux.config/type :crux.config/string}})})

(def tx-log-module
  {:start-fn start-tx-log
   :deps     [:crux.hbase/tx-log-kv]})

(def object-store-module
  {:start-fn start-object-store
   :deps     [:crux.hbase/object-kv]
   :args     {:crux.object-store/doc-cache-size crux.object-store/doc-cache-size-opt}})


(def tx-log-consumer-module
  {:start-fn start-tx-log-consumer
   :deps     [:crux.hbase/tx-log-kv :crux.node/indexer]})

(def topology
  (merge
    n/base-topology
    {:crux.hbase/hbase-connection {:start-fn start-hbase-client}
     :crux.hbase/tx-log-kv        crux.hbase/tx-log-kv-module
     :crux.hbase/tx-log-consumer  crux.hbase/tx-log-consumer-module
     :crux.node/kv-store          crux.hbase/index-kv-module
     :crux.node/tx-log            crux.hbase/tx-log-module
     :crux.node/object-store      crux.object-store/kv-object-store
     }))
