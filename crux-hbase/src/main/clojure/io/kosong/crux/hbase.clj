(ns io.kosong.crux.hbase
  (:require [crux.kv :as kv]
            [crux.memory :as mem]
            [crux.system :as sys])
  (:import (io.kosong.crux.hbase HBaseIterator)
           (org.apache.hadoop.hbase TableName)
           (org.apache.hadoop.hbase.client Put Delete Get Result ResultScanner Scan Connection)
           (java.io Closeable)
           (java.util LinkedList)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.hbase.client Connection ConnectionFactory TableDescriptorBuilder ColumnFamilyDescriptorBuilder)
           (org.apache.hadoop.hbase HBaseConfiguration NamespaceDescriptor NamespaceExistException NamespaceExistException)
           (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.security UserGroupInformation)))

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
