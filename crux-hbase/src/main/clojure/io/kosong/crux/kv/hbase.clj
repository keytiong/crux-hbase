(ns io.kosong.crux.kv.hbase
  (:require [crux.node :as n]
            [crux.kv :as kv]
            [crux.memory :as mem]
            [crux.lru :as lru]
            [crux.document-store :as ds]
            [crux.standalone :as standalone])
  (:import (org.apache.hadoop.hbase TableName)
           (org.apache.hadoop.hbase.client Put Delete Get Table Result ResultScanner Scan Connection)
           (java.io Closeable)
           (java.util LinkedList)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.hbase.client ConnectionFactory TableDescriptorBuilder ColumnFamilyDescriptorBuilder)
           (org.apache.hadoop.hbase HBaseConfiguration NamespaceDescriptor NamespaceExistException NamespaceExistException)
           (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.hbase.util Bytes)))


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

(defn- iterator->key [^io.kosong.crux.hbase.HBaseKvIterator i]
  (when (.isValid i)
    (mem/->off-heap (.key i))))
;;
;; KvIterator
;;
(defrecord HBaseKvIterator [^io.kosong.crux.hbase.HBaseKvIterator i]
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
(defrecord HBaseKvSnapshot [^Table table family qualifier]
  kv/KvSnapshot
  (new-iterator [_]
    (let [i (io.kosong.crux.hbase.HBaseKvIterator. table family qualifier)]
      (->HBaseKvIterator i)))

  (get-value [_ k]
    (let [g         (Get. (mem/->on-heap k))
          ^Result r (.get table g)]
      (some-> r
              (.getValue family qualifier)
              (mem/->off-heap))))

  Closeable
  (close [_]
    nil))


(defn- count-keys [i ^ResultScanner scanner]
  (let [result (some-> scanner (.next))]
    (if (or (nil? result)
            (.isEmpty result))
      i
      (recur (inc i) scanner))))

;;
;; KvStore
;;
(defrecord HBaseKvStore [^Connection connection ^Table table family qualifier]
  kv/KvStore
  (store [this kvs]
    (let [puts (LinkedList.)]
      (doseq [[k v] kvs]
        (.add puts (doto (Put. (mem/->on-heap k))
                     (.addColumn family qualifier (mem/->on-heap v)))))
      (.put table puts)))

  (new-snapshot [this]
    (let [table-name (.getName table)
          table      (.getTable connection table-name)]
      (->HBaseKvSnapshot table family qualifier)))

  (delete [this ks]
    (let [deletes (LinkedList.)]
      (doseq [k ks]
        (.add deletes (Delete. (mem/->on-heap k))))
      (.delete table deletes)))

  (fsync [this]
    nil)

  (backup [this _]
    nil)

  (compact [this]
    nil)

  (count-keys [this]
    (let [scanner (.getScanner table (Scan.))]
      (count-keys 0 scanner)))

  (db-dir [this]
    (-> table (.getName) (.getNameAsString)))

  (kv-name [this]
    (.getName (class this)))

  Closeable
  (close [this]
    (.close table)))

(defn- start-hbase-connection [_ {:keys [::hbase-config]}]
  (let [hadoop-conf (reduce-kv (fn [conf k v]
                                 (doto conf (.set k v)))
                               (Configuration.)
                               hbase-config)]
    (-> (HBaseConfiguration/create hadoop-conf)
        (ConnectionFactory/createConnection))))

(defn- start-hbase-kv [connection namespace table family qualifier]
  (ensure-table connection namespace table family)
  (let [table-name (TableName/valueOf ^bytes (Bytes/toBytesBinary namespace)
                                      ^bytes (Bytes/toBytesBinary table))
        table      (.getTable connection table-name)
        family     (Bytes/toBytesBinary family)
        qualifier  (Bytes/toBytesBinary qualifier)]
    (->HBaseKvStore connection table family qualifier)))

(defn- start-kv-store [{:keys [::connection]}
                       {:keys [::namespace
                               ::kv-store-table
                               ::family
                               ::qualifier]}]
  (start-hbase-kv connection namespace kv-store-table family qualifier))

(defn- start-event-log-kv-store [{:keys [::connection]}
                                 {:keys [::namespace
                                         ::event-log-kv-store-table
                                         ::family
                                         ::qualifier]}]
  (start-hbase-kv connection namespace event-log-kv-store-table family qualifier))

(defn- start-document-store [{:keys [::connection]}
                             {:keys [::namespace
                                     ::document-store-table
                                     ::family
                                     ::qualifier
                                     :crux.document-store/doc-cache-size]}]
  (let [kv-store  (start-hbase-kv connection namespace document-store-table family qualifier)
        cache     (lru/new-cache doc-cache-size)
        doc-store (ds/->KvDocumentStore kv-store)]
    (ds/->CachedDocumentStore cache doc-store)))

(defn- start-event-log-document-store [{:keys [::connection]}
                                       {:keys [::namespace
                                               ::event-log-document-store-table
                                               ::family
                                               ::qualifier
                                               :crux.document-store/doc-cache-size]}]
  (let [kv-store (start-hbase-kv connection namespace event-log-document-store-table family qualifier)
        cache (lru/new-cache doc-cache-size)
        doc-store (ds/->KvDocumentStore kv-store)]
    (ds/->CachedDocumentStore cache doc-store)))

(defn- start-event-log [{:keys [::event-log-kv-store ::event-log-document-store]} _]
  (standalone/->EventLog event-log-kv-store event-log-document-store))

(def hbase-client-options
  {::hbase-client-config
   {:doc              "HBase client configuration"
    :default          {}
    :crux.config/type :crux.config/string-map}})

(def ^:private default-options
  {::family
   {:doc              "HBase column family name"
    :default          "cf"
    :crux.config/type :crux.config/string}
   ::namespace
   {:doc              "HBase namespace"
    :default          "crux"
    :crux.config/type :crux.config/string}
   ::qualifier
   {:doc              "HBase column qualifier name"
    :default          "val"
    :crux.config/type :crux.config/string}})


(def connection
  {:start-fn start-hbase-connection
   :args     hbase-client-options})

(def kv-store
  {:start-fn start-kv-store
   :deps     [::connection]
   :args     (merge default-options
                    {::kv-store-table
                     {:doc              "HBase table name"
                      :default          "kv-store"
                      :crux.config/type :crux.config/string}})})

(def event-log-kv-store
  {:start-fn start-event-log-kv-store
   :deps     [::connection]
   :args     (merge default-options
                    {::event-log-kv-store-table
                     {:doc              "Event log KV store table name"
                      :default          "event-log-kv-store"
                      :crux.config/type :crux.config/string}})})

(def event-log-document-store
  {:start-fn start-event-log-document-store
   :deps     [::connection]
   :args     (merge default-options
                    {::event-log-document-store-table
                     {:doc              "HBase event log document store table name"
                      :default          "event-log-document-store"
                      :crux.config/type :crux.config/string}}
                    {:doc/doc-cache-size ds/doc-cache-size-opt})})

(def event-log
  {:start-fn start-event-log
   :deps     [::event-log-kv-store ::event-log-document-store]})

(def topology
  (merge standalone/topology
         {::n/kv-store               kv-store
          ::event-log-kv-store       event-log-kv-store
          ::event-log-document-store event-log-document-store
          :crux.standalone/event-log event-log
          ::connection               connection}))
