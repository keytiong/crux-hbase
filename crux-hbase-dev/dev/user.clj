(ns user
  (:require [crux.hbase.embedded]
            [crux.kv.hbase]
            [crux.api :as crux]
            [integrant.core :as i]
            [integrant.repl.state :refer [system]]
            [integrant.repl :as ir :refer [clear go suspend resume halt reset reset-all]])
  (:import (crux.api ICruxAPI)))


(defmethod i/init-key :node [_ node-opts]
  (crux/start-node node-opts))

(defmethod i/halt-key! :node [_ ^ICruxAPI node]
  (.close node))

(defmethod i/init-key :embedded-hbase [_ hbase-opts]
  (let [hbase (crux.hbase.embedded/start-embedded-hbase hbase-opts)]
    hbase))

(defmethod i/halt-key! :embedded-hbase [_ hbase]
  (.close hbase))

(def config
  {:embedded-hbase {:crux.hbase.embedded/zookeeper-data-dir "zk-data-dir"
                    :crux.hbase.embedded/hbase-data-dir     "hbase-data-dir"
                    :crux.hbase.embedded/hbase-config       {"hbase.tmp.dir" "./hbase-tmp"
                                                             "hbase.rootdir" "./hbase"}}

   :node           {:crux.node/topology                    ['crux.kv.hbase/topology]
                    :crux.kv/check-and-store-index-version true
                    :crux.document-store/doc-cache-size    131072}})

(ir/set-prep! (fn [] config))

(defn crux-node []
  (:node system))