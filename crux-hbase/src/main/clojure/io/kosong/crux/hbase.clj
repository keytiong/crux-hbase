(ns io.kosong.crux.hbase
  (:require [crux.tx :as tx]
            [crux.db :as db]
            [crux.kv :as kv]
            [crux.memory :as mem]
            [crux.system :as sys]
            [crux.tx.event :as txe]
            [clojure.tools.logging :as log])
  (:import (io.kosong.crux.hbase HBaseIterator)
           (org.apache.hadoop.hbase TableName)
           (org.apache.hadoop.hbase.client Put Delete Get Result Scan Connection)
           (java.io Closeable)
           (java.util LinkedList UUID)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.hbase.client Connection ConnectionFactory TableDescriptorBuilder ColumnFamilyDescriptorBuilder)
           (org.apache.hadoop.hbase HBaseConfiguration NamespaceDescriptor NamespaceExistException NamespaceExistException)
           (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.security UserGroupInformation)
           (org.apache.curator.framework CuratorFrameworkFactory)
           (org.apache.curator.retry ExponentialBackoffRetry)
           (org.apache.curator.framework.recipes.leader LeaderSelector LeaderSelectorListener LeaderSelectorListenerAdapter)))

(defn ensure-namespace [^Connection conn ^String ns]
  (let [ns-descriptor (-> ns
                        (NamespaceDescriptor/create)
                        (.build))
        admin         (.getAdmin conn)]
    (try
      (.createNamespace admin ^NamespaceDescriptor ns-descriptor)
      (catch NamespaceExistException _))))

(defn ensure-table [^Connection conn ^String ns ^String table ^String family]
  (ensure-namespace conn ns)
  (let [table-name (TableName/valueOf (Bytes/toBytesBinary ns)
                     (Bytes/toBytesBinary table))
        cf-desc    (-> (ColumnFamilyDescriptorBuilder/newBuilder (Bytes/toBytesBinary family))
                     (.build))
        table-desc (-> (TableDescriptorBuilder/newBuilder table-name)
                     (.setColumnFamily cf-desc)
                     (.build))]
    (with-open [admin (.getAdmin conn)]
      (when-not (.tableExists admin table-name)
        (.createTable admin table-desc)))))

(defn- iterator->key [^HBaseIterator i]
  (when (.isValid i)
    (mem/->off-heap (.key i))))
;;
;; KvIterator
;;
(defrecord HBaseKvIterator [^HBaseIterator i]
  kv/KvIterator
  (seek [_ k]
    (.seek i (mem/->on-heap k))
    (iterator->key i))

  (next [_]
    (.next i)
    (iterator->key i))

  (prev [_]
    (.prev i)
    (iterator->key i))

  (value [_]
    (mem/->off-heap (.value i)))

  Closeable
  (close [_]
    (.close i)))

;;
;; KvSnapshot
;;
(defrecord HBaseKvSnapshot [^Connection connection ^TableName table-name family qualifier]
  kv/KvSnapshot
  (new-iterator [_]
    (let [i (HBaseIterator. connection table-name family qualifier)]
      (->HBaseKvIterator i)))

  (get-value [_ k]
    (with-open [table (.getTable connection table-name)]
      (let [g         (Get. (mem/->on-heap k))
            ^Result r (.get table g)]
        (some-> r
          (.getValue family qualifier)
          (mem/->off-heap)))))

  Closeable
  (close [_]
    nil))

;;
;; KvStore
;;
(defrecord HBaseKvStore [^Connection connection ^TableName table-name family qualifier]
  kv/KvStore
  (store [_ kvs]
    (with-open [table (.getTable connection table-name)]
      (let [puts    (LinkedList.)
            deletes (LinkedList.)]
        (doseq [[k v] kvs]
          (if v
            (.add puts (doto (Put. (mem/->on-heap k))
                         (.addColumn family qualifier (mem/->on-heap v))))
            (.add deletes (Delete. (mem/->on-heap k)))))
        (when-not (.isEmpty puts)
          (.put table puts))
        (when-not (.isEmpty deletes)
          (.delete table deletes)))))

  (new-snapshot [_]
    (->HBaseKvSnapshot connection table-name family qualifier))

  (fsync [_]
    nil)

  (compact [_]
    nil)

  ;;
  ;; Naive implementation of HBase row count. It will take
  ;; a LONG time in a large table
  ;;
  (count-keys [_]
    (with-open [table   (.getTable connection table-name)
                scanner (.getScanner table (doto (Scan.)
                                             (.setCaching 10000)))]
      (count (seq scanner))))

  (db-dir [_]
    (.getNameAsString table-name))

  (kv-name [this]
    (.getName (class this)))

  Closeable
  (close [_]
    nil))

(defn start-hbase-connection [hbase-config]
  (when-not (UserGroupInformation/isInitialized)
    (UserGroupInformation/setConfiguration hbase-config))
  (ConnectionFactory/createConnection hbase-config))

(defn- start-hbase-kv [connection namespace table family qualifier]
  (ensure-table connection namespace table family)
  (let [table-name (TableName/valueOf ^bytes (Bytes/toBytesBinary namespace)
                     ^bytes (Bytes/toBytesBinary table))
        family     (Bytes/toBytesBinary family)
        qualifier  (Bytes/toBytesBinary qualifier)]
    (->HBaseKvStore connection table-name family qualifier)))

(defn ->hbase-config {::sys/args {:properties {:doc      "HBase configuration properties"
                                               :require? false
                                               :default  {}
                                               :spec     ::sys/string-map}}}
  [{:keys [properties]}]
  (let [hadoop-conf (reduce-kv (fn [^Configuration conf k v]
                                 (doto conf (.set k v)))
                      (Configuration.)
                      properties)]
    (HBaseConfiguration/create hadoop-conf)))

(defn ->hbase-connection {::sys/deps {:hbase-config {:crux.module '->hbase-config}}}
  [{:keys [hbase-config]}]
  (start-hbase-connection hbase-config))

(defn ->kv-store {::sys/deps {:hbase-connection {:crux.module '->hbase-connection}}
                  ::sys/args {:table         {:doc       "Table name"
                                              :required? true
                                              :spec      ::sys/string}
                              :family        {:doc       "Column family name"
                                              :required? true
                                              :default   "cf"
                                              :spec      ::sys/string}
                              :namespace     {:doc       "HBase namespace"
                                              :required? true
                                              :default   "crux"
                                              :spec      ::sys/string}
                              :qualifier     {:doc       "HBase column qualifier"
                                              :required? true
                                              :default   "val"
                                              :spec      ::sys/string}
                              :create-table? {:doc       "Create table if it does not exist"
                                              :required? true
                                              :default   true
                                              :spec      ::sys/boolean}}}
  [{:keys [hbase-connection namespace table family qualifier create-table?]}]
  (do
    (when create-table?
      (ensure-table hbase-connection namespace table family))
    (start-hbase-kv hbase-connection namespace table family qualifier)))

(defn ->curator
  {::sys/args {:zookeeper-quorum   {:doc       "Zookeeper connection string"
                                    :required? true
                                    :spec      ::sys/string}
               :session-timeout    {:doc       "Session timeout in milliseconds"
                                    :required? false
                                    :default   60000
                                    :spec      ::sys/int}
               :connection-timeout {:doc       "Connection timeout in milliseconds"
                                    :required? false
                                    :default   60000
                                    :spec      ::sys/int}
               :retry-base-sleep   {:doc      "Exponential backoff retry base sleep time in milliseconds"
                                    :require? false
                                    :default  2000
                                    :spec     ::sys/int}
               :retry-max-count    {:doc       "Exponential backoff retry maximum count"
                                    :required? false
                                    :default   10
                                    :spec      ::sys/int}}}
  [{:keys [zookeeper-quorum session-timeout connection-timeout retry-base-sleep retry-max-count]}]
  (doto (CuratorFrameworkFactory/newClient zookeeper-quorum
          session-timeout
          connection-timeout
          (ExponentialBackoffRetry. retry-base-sleep retry-max-count))
    (.start)))

;;
;; Based on ingest-tx function in https://github.com/juxt/crux/blob/master/crux-core/src/crux/kv/tx_log.clj
;;
(defn- ingest-tx [tx-ingester tx tx-events]
  (let [in-flight-tx (db/begin-tx tx-ingester tx nil)]
    (if (db/index-tx-events in-flight-tx tx-events)
      (db/commit in-flight-tx)
      (db/abort in-flight-tx))))

;;
;; Based on ->tx-log function in https://github.com/juxt/crux/blob/master/crux-core/src/crux/kv/tx_log.clj
;;
(defn- run-tx-ingest-executor [tx-ingester tx-log index-store]
  (try
    (while :true
      (let [latest-submitted-tx-id (::tx/tx-id (db/latest-submitted-tx tx-log))
            latest-completed-tx-id (::tx/tx-id (db/latest-completed-tx index-store))]
        (with-open [txs (db/open-tx-log tx-log latest-completed-tx-id)]
          (doseq [tx (iterator-seq txs)
                  :while (<= (::tx/tx-id tx) (or latest-submitted-tx-id 0))]
            (ingest-tx tx-ingester
              (select-keys tx [::tx/tx-id ::tx/tx-time])
              (::txe/tx-events tx)))))
      (Thread/sleep 100))
    (catch InterruptedException _
      (Thread/interrupted)
      (log/info "tx-ingest-executor interrupted"))))

(defrecord TxIngestExecutor [leader-selector]
  Closeable
  (close [_]
    (if (.hasLeadership leader-selector)
      (do (.interruptLeadership leader-selector)
          (Thread/sleep 1000)
          (.close leader-selector))
      (.close leader-selector))))

(defn- tx-ingest-executor-thread [tx-ingester tx-log index-store]
  (let [runnable #(run-tx-ingest-executor tx-ingester
                    tx-log index-store)]
    (Thread. ^Runnable runnable "tx-ingest-executor")))

(defn ->tx-ingest-executor
  {::sys/deps {:index-store :crux/index-store
               :tx-log      :crux/tx-log
               :tx-ingester :crux/tx-ingester
               :curator     :curator}
   ::sys/args {:mutex-path {:doc      "Zookeeper leader mutex path"
                            :require? false
                            :default  "/crux/tx-ingest-executor"}}}
  [{:keys [index-store tx-log tx-ingester curator mutex-path]}]
  (let [leader-listener (proxy [LeaderSelectorListenerAdapter] []
                          (takeLeadership [_]
                            (log/info "Taking tx-ingest-executor leadership")
                            (let [executor (tx-ingest-executor-thread tx-ingester
                                             tx-log index-store)]
                              (try
                                (.start executor)
                                (.join executor)
                                (catch InterruptedException _
                                  (.interrupt executor)
                                  (.join executor))))))
        leader-selector (doto (LeaderSelector. curator mutex-path leader-listener)
                          (.setId (str (UUID/randomUUID)))
                          (.autoRequeue)
                          (.start))]
    (->TxIngestExecutor leader-selector)))
