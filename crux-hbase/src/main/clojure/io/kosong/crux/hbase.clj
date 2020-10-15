(ns io.kosong.crux.hbase
  (:require [crux.kv :as kv]
            [crux.memory :as mem]
            [crux.lru :as lru]
            [crux.document-store :as ds]
            [crux.system :as sys]
            [clojure.spec.alpha :as s])
  (:import (io.kosong.crux.hbase HBaseIterator)
           (org.apache.hadoop.hbase TableName)
           (org.apache.hadoop.hbase.client Put Delete Get Table Result ResultScanner Scan Connection)
           (java.io Closeable)
           (java.util LinkedList)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.hbase.client Connection ConnectionFactory TableDescriptorBuilder ColumnFamilyDescriptorBuilder)
           (org.apache.hadoop.hbase HBaseConfiguration NamespaceDescriptor NamespaceExistException NamespaceExistException)
           (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.hbase.util Bytes)))


(s/def ::connection #(instance? Connection %))

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
(defrecord HBaseKvSnapshot [^Table table family qualifier]
  kv/KvSnapshot
  (new-iterator [_]
    (let [i (HBaseIterator. table family qualifier)]
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

(defn start-hbase-connection [hbase-config]
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

(defn ->kv-store {::sys/args {:connection {:doc       "HBase connection"
                                           :required? true
                                           :spec      ::connection}
                              :table      {:doc       "Table name"
                                           :required? true
                                           :spec      ::sys/string}
                              :family     {:doc     "Column family name"
                                           :default "cf"
                                           :spec    ::sys/string}
                              :namespace  {:doc     "Hbase namespace"
                                           :default "crux"
                                           :spec    ::sys/string}
                              :qualifier  {:doc     "Hbase column qualifier"
                                           :default "val"
                                           :spec    ::sys/string}}}
  [{:keys [table family namespace qualifier connection]}]
  (do
    (ensure-table connection namespace table family)
    (start-hbase-kv connection namespace table family qualifier)))
