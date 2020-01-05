(ns crux.kv.hbase
  (:require [crux.kv]
            [crux.kv :as kv])
  (:import (org.apache.hadoop.hbase TableName)
           (org.apache.hadoop.hbase.client Put Delete Get Table Result ResultScanner Scan)
           (java.io Closeable)
           (java.util LinkedList)
           (org.apache.hadoop.hbase.util Bytes)))

(defn- iterator->key [^io.kosong.crux.hbase.HBaseKvIterator i]
  (when (.isValid i)
    (.key i)))
;;
;; KvIterator
;;
(defrecord HBaseKvIterator [^io.kosong.crux.hbase.HBaseKvIterator i]
  kv/KvIterator
  (seek [_ k]
    (.seek i k)
    (iterator->key i))

  (next [_]
    (.next i)
    (iterator->key i))

  (prev [_]
    (.prev i)
    (iterator->key i))

  (value [_]
    (.value i))

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
    (let [g         (Get. k)
          ^Result r (.get table g)]
      (some-> r
              (.getValue family qualifier))))

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
(defrecord HBaseKvStore [connection namespace-str table-str family-str qualifier-str]
  kv/KvStore
  (open [this options]
    (let [table-name (TableName/valueOf ^bytes (Bytes/toBytesBinary namespace-str)
                                        ^bytes (Bytes/toBytesBinary table-str))
          table      (.getTable connection table-name)]
      (-> this
          (assoc :table table)
          (assoc :family (Bytes/toBytesBinary family-str))
          (assoc :qualifier (Bytes/toBytesBinary qualifier-str)))))

  (store [{:keys [family qualifier table]} kvs]
    (let [puts (LinkedList.)]
      (doseq [[k v] kvs]
        (.add puts (doto (Put. k)
                     (.addColumn family qualifier v))))
      (.put table puts)))

  (new-snapshot [{:keys [table family qualifier]}]
    (let [table-name (.getName table)
          table      (.getTable connection table-name)]
      (->HBaseKvSnapshot table family qualifier)))

  (delete [{:keys [table]} ks]
    (let [deletes (LinkedList.)]
      (doseq [k ks]
        (.add deletes (Delete. k)))
      (.delete table deletes)))

  (fsync [_]
    nil)

  (backup [_ _]
    nil)

  (count-keys [{:keys [table]}]
    (let [scanner (.getScanner table (Scan.))]
      (count-keys 0 scanner)))

  (db-dir [_]
    table-str)

  (kv-name [this]
    (.getName (class this)))

  Closeable
  (close [{:keys [table]}]
    (.close table)))
