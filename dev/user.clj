(ns user
  (:require [io.kosong.crux.hbase.embedded]
            [io.kosong.crux.kv.hbase :as kv]
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

   :node           {:crux.node/topology                    ['io.kosong.crux.kv.hbase/topology]
                    :crux.kv/check-and-store-index-version true
                    :crux.document-store/doc-cache-size    131072}})

(ir/set-prep! (fn [] config))

(defn crux-node []
  (:node system))
