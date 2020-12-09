(ns dev
  (:require [io.kosong.crux.hbase.embedded :as ehb]
            [io.kosong.crux.hbase :as hb]
            [crux.api :as crux]
            [integrant.core :as ig]
            [integrant.repl.state :refer [system]]
            [integrant.repl :as ir :refer [clear go suspend resume halt reset reset-all]]
            [clojure.java.io :as io])
  (:import (crux.api ICruxAPI)))

(def dev-node-dir
  (io/file "dev/dev-node"))

(defmethod ig/init-key ::crux [_ node-opts]
  (crux/start-node (dissoc node-opts :hbase)))

(defmethod ig/halt-key! ::crux [_ ^ICruxAPI node]
  (.close node))

(defmethod ig/init-key ::embedded-hbase [_ {:keys [hbase-dir hbase-config zk-port]}]
  (let [hbase-config (merge {"hbase.rootdir" (.getPath hbase-dir)}
                            hbase-config)]
    (ehb/start-embedded-hbase #::ehb{:zookeeper-port zk-port
                                     :hbase-config   hbase-config})))

(defmethod ig/halt-key! ::embedded-hbase [_ hbase]
  (.close hbase))

(defmethod ig/init-key ::embedded-zookeeper [_ {:keys [zk-data-dir zk-port]}]
  (ehb/start-embedded-zookeeper #::ehb{:zookeeper-data-dir zk-data-dir
                                       :zookeeper-port     zk-port}))

(defmethod ig/halt-key! ::embedded-zookeeper [_ zk]
  (.close zk))

(defmethod ig/init-key ::hbase-connection [_ {:keys [hbase-config]}]
  (hb/start-hbase-connection hbase-config))

(defmethod ig/halt-key! ::hbase-connection [_ conn]
  (.close conn))

(def config
  {::embedded-zookeeper {:zk-data-dir (io/file dev-node-dir "zookeeper")
                         :zk-port     2181}

   ::embedded-hbase     {:hbase-dir    (io/file dev-node-dir "hbase")
                           :deps         [(ig/ref ::embedded-zookeeper)]
                           :hbase-config {"hbase.master.info.port"                       "-1"
                                          "hbase.regionserver.info.port"                 "-1"
                                          "hbase.master.start.timeout.localHBaseCluster" "60000"
                                          "hbase.unsafe.stream.capability.enforce"       "false"
                                          "hbase.zookeeper.quorum"                       "127.0.0.1:2181"}}

   ::hbase-connection   {:deps         [(ig/ref ::embedded-hbase)]
                         :hbase-config {"hbase.zookeeper.quorum" "127.0.0.1:2181"}}

   ::crux               {:crux/index-store    {:kv-store {:crux/module 'io.kosong.crux.hbase/->kv-store
                                                          :connection  (ig/ref ::hbase-connection)
                                                          :table       "index-store"}}
                         :crux/document-store {:kv-store {:crux/module 'io.kosong.crux.hbase/->kv-store
                                                          :connection  (ig/ref ::hbase-connection)
                                                          :table       "document-store"}}
                         :crux/tx-log         {:kv-store {:crux/module 'io.kosong.crux.hbase/->kv-store
                                                          :connection  (ig/ref ::hbase-connection)
                                                          :table       "tx-log"}}}})

(ir/set-prep! (fn [] config))

(defn crux-node []
  (::crux system))