(ns dev
  (:require [io.kosong.crux.hbase.embedded :as ehb]
            [crux.api :as crux]
            [integrant.core :as ig]
            [integrant.repl.state :refer [system]]
            [integrant.repl :as ir :refer [clear go suspend resume halt reset reset-all]]
            [clojure.java.io :as io])
  (:import crux.api.ICruxAPI))

(defonce embedded-cluster nil)

(def dev-node-dir
  (io/file "dev/dev-node"))

(defmethod ig/init-key ::crux [_ node-opts]
  (crux/start-node (dissoc node-opts :deps)))

(defmethod ig/halt-key! ::crux [_ ^ICruxAPI node]
  (.close node))

(defmethod ig/init-key ::embedded-hbase [_ {:keys [hbase-config]}]
  (ehb/start-embedded-hbase #::ehb{:hbase-config hbase-config}))

(defmethod ig/halt-key! ::embedded-hbase [_ hbase]
  (.close hbase))

(defmethod ig/init-key ::embedded-zookeeper [_ {:keys [zk-data-dir zk-port]}]
  (ehb/start-embedded-zookeeper #::ehb{:zookeeper-data-dir zk-data-dir
                                       :zookeeper-port     zk-port}))

(defmethod ig/halt-key! ::embedded-zookeeper [_ zk]
  (.close zk))

(def embedded-cluster-config
  {::embedded-zookeeper {:zk-data-dir (io/file dev-node-dir "zookeeper")
                         :zk-port     2181}

   ::embedded-hbase     {:deps         [(ig/ref ::embedded-zookeeper)]

                         :hbase-config {:properties {"hbase.rootdir"                                (.getPath (io/file dev-node-dir "hbase"))
                                                     "hbase.master.info.port"                       "-1"
                                                     "hbase.regionserver.info.port"                 "-1"
                                                     "hbase.master.start.timeout.localHBaseCluster" "60000"
                                                     "hbase.unsafe.stream.capability.enforce"       "false"
                                                     "hbase.zookeeper.quorum"                       "127.0.0.1:2181"}}}})

(def crux-hbase-config
  {::crux {:hbase-config        {:crux/module 'io.kosong.crux.hbase/->hbase-config
                                 :properties  {"hbase.zookeeper.quorum" "127.0.0.1:2181"}}

           :hbase-connection    {:crux/module  'io.kosong.crux.hbase/->hbase-connection
                                 :hbase-config :hbase-config}

           :crux/index-store    {:kv-store {:crux/module      'io.kosong.crux.hbase/->kv-store
                                            :hbase-connection :hbase-connection
                                            :table            "index-store"}}
           :crux/document-store {:kv-store {:crux/module      'io.kosong.crux.hbase/->kv-store
                                            :hbase-connection :hbase-connection
                                            :table            "document-store"}}
           :crux/tx-log         {:kv-store {:crux/module      'io.kosong.crux.hbase/->kv-store
                                            :hbase-connection :hbase-connection
                                            :table            "tx-log"}}}}
  )

(ir/set-prep! (fn [] crux-hbase-config))

(defn start-embedded-cluster []
  (alter-var-root #'embedded-cluster
    (fn [cluster]
      (if-not cluster
        (ig/init embedded-cluster-config)
        cluster))))

(defn stop-embedded-cluster []
  (alter-var-root #'embedded-cluster
    (fn [cluster]
      (when-not cluster
        (ig/halt! cluster)))))

(defn crux-node []
  (::crux system))
