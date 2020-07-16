(ns crux.kv.hbase
  (:require  [crux.node :as n]
             [crux.kv :as kv]
             [crux.memory :as mem])
  (:import (org.apache.hadoop.hbase TableName)
           (org.apache.hadoop.hbase.client Put Delete Get Table Result ResultScanner Scan Connection)
           (java.io Closeable)
           (java.util LinkedList)
           (org.apache.hadoop.hbase.util Bytes)
           (org.apache.hadoop.hbase.client ConnectionFactory)
           (org.apache.hadoop.hbase HBaseConfiguration TableName)
           (org.apache.hadoop.conf Configuration)
           (org.apache.hadoop.hbase.util Bytes)))

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
    (.close table)))


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

  (count-keys [this]
    (let [scanner (.getScanner table (Scan.))]
      (count-keys 0 scanner)))

  (db-dir [this]
    (-> table (.getName) (.getNameAsString)))

  (kv-name [this]
    (.getName (class this)))

  Closeable
  (close [{:keys [table]}]
    (.close table)))


(defn- start-index-kv [_
                       {:keys [::namespace
                               ::index-table
                               ::index-family
                               ::index-qualifier]
                        :as   options}]
  (let [conn (let [c (doto (Configuration.)
                       (.set "hbase.rootdir" "./hbase"))]
               (-> (HBaseConfiguration/create c)
                   (ConnectionFactory/createConnection)))
        table-name (TableName/valueOf ^bytes (Bytes/toBytesBinary namespace)
                                      ^bytes (Bytes/toBytesBinary index-table))
        table      (.getTable conn table-name)
        family (Bytes/toBytesBinary index-family)
        qualifier (Bytes/toBytesBinary index-qualifier)
        kv (crux.kv.hbase/->HBaseKvStore conn table family qualifier)]
    kv))

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


(def kv
  {:start-fn start-index-kv
   :args     (merge hbase-options
                    {:crux.hbase/index-table
                     {:doc              "Index table name"
                      :default          "index"
                      :crux.config/type :crux.config/string}})})

(def kv-store {:crux.node/kv-store kv})