(ns user
  (:require [io.kosong.crux.hbase.embedded]
            [crux.api :as crux]
            [integrant.core :as i]
            [integrant.repl.state :refer [system]]
            [integrant.repl :as ir :refer [clear go suspend resume halt reset reset-all]])
  (:import (crux.api ICruxAPI)))


(defmethod i/init-key :node [_ node-opts]
  (crux/start-node node-opts))

(defmethod i/halt-key! :node [_ ^ICruxAPI node]
  (.close node))

(defmethod i/init-key :hbase [_ hbase-opts]
  (let [hbase (io.kosong.crux.hbase.embedded/start-embedded-hbase hbase-opts)]
    hbase))

(defmethod i/halt-key! :hbase [_ hbase]
  (.close hbase))

(def config
  {:hbase {:io.kosong.crux.hbase.embedded/zookeeper-data-dir "./data/zk-data-dir"
           :io.kosong.crux.hbase.embedded/hbase-config       {"hbase.tmp.dir" "./data/hbase-tmp"
                                                              "hbase.rootdir" "./data/hbase"}}

   :node  {:crux/index-store    {:kv-store {:crux/module 'io.kosong.crux.kv.hbase/->kv-store
                                            :table       "index-store"}}
           :crux/document-store {:kv-store {:crux/module 'io.kosong.crux.kv.hbase/->kv-store
                                            :table       "document-store"}}
           :crux/tx-log         {:kv-store {:crux/module 'io.kosong.crux.kv.hbase/->kv-store
                                            :table       "tx-log"}}}})

(ir/set-prep! (fn [] config))

(defn crux-node []
  (:node system))
