(defproject io.kosong.crux/crux-hbase-embedded "0.1.0"
  :description "Embedded HBase for development and testing"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:parent "../crux-hbase-parent"}
  :scm {:dir ".."}
  :source-paths ^:replace ["src/main/clojure"]
  :java-source-paths ^:replace ["src/main/java"]
  :test-paths ^:replace ["src/test/clojure"]
  :dependencies [[org.apache.hbase/hbase-client _ :exclusions [com.google.guava/guava
                                                               io.netty/netty
                                                               net.minidev/json-smart]]
                 [org.apache.hbase/hbase-server _ :exclusions [com.google.guava/guava
                                                               io.netty/netty
                                                               org.glassfish/javax.el
                                                               net.minidev/json-smart]]
                 [org.apache.zookeeper/zookeeper "3.6.2"]
                 [org.eclipse.jetty/jetty-util "9.4.35.v20201120"]

                 ;; dependency conflict resolution
                 [commons-logging "1.2"]])
