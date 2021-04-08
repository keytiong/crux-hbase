# crux-hbase
This is a [Crux](https://opencrux.com) key value store implementation with
[HBase](https://hbase.apache.org). HBase KV Store is horizontally scalable.
It is intended Crux deployment on environment like Kubernetes where Crux
nodes can be provisioned dynamically on any nodes in the cluster without
requiring local index store.

## Usage

### Maven
```xml
<dependency>
  <groupId>io.kosong.crux</groupId>
  <artifactId>crux-hbase</artifactId>
  <version>0.3.0-SNAPSHOT</version>
</dependency>

```
### Leiningen
```clojure
[io.kosong.crux.hbase/crux-hbase "0.3.0-SNAPSHOT"]
```


## Module Configuration

Refer to Crux [reference](https://opencrux.com/reference/installation.html) for
details on how to configure Crux index store, document store and transaction log.

The first step is to specify the HBase client configuration with the
`io.kosong.crux.hbase/->hbase-config` module.

This follows with the use of
`io.kosong.crux.hbase/->hbase-connection` module to create the HBase connection.

And then we can use the `io.kosong.crux.hbase/->kv-store` module to configure
the desired Crux components (index store, document store or transaction log)
to use HBase KV Store.

Below is an example of configuring Crux to use HBase KV Stores for all three major
components (index store, document store and transaction log).

The configuration allows multiple crux nodes to connect to the same distributed
HBase KV index store with single curator coordinated cluster-wide tx-ingester
executor to ingest the transactions into the shared distributed index store.

```clojure
{
  ;; create a HBaseConfiguration
  :hbase-config
   {:crux/module 'io.kosong.crux.hbase/->hbase-config
    :properties  {"hbase.zookeeper.quorum" "127.0.0.1:2181"}}
 
  ;; create a shared HBase connection
  :hbase-connection
    {:crux/module  'io.kosong.crux.hbase/->hbase-connection
     :hbase-config :hbase-config}

  ;; create a curator client to coordinate singleton tx-ingester
  :curator
    {:crux/module      'io.kosong.crux.hbase/->curator
     :zookeeper-quorum "127.0.0.1:2181"}
 
  ;; cluster-wide singleton tx-ingest-executor with curator
  ;; to ensure that there is only one active tx-ingester
  ;; updating the remote hbase index-store at any one time
  :tx-ingest-executor
   {:crux/module       'io.kosong.crux.hbase/->tx-ingest-executor
    :curator           :curator}
 
  :crux/index-store
    {:kv-store {:crux/module       'io.kosong.crux.hbase/->kv-store
                :hbase-connection  :hbase-connection
                :table             "index-store"}}

  :crux/document-store
    {:kv-store {:crux/module       'io.kosong.crux.hbase/->kv-store
                :hbase-connection  :hbase-connection
                :table             "document-store"}}

   ;; HBase KV tx-log in ingest only mode as tx-ingester is managed
   ;; by tx-ingest-executor
  :crux/tx-log
    {:crux/module 'crux.kv.tx-log/->ingest-only-tx-log
     :kv-store    {:crux/module       'io.kosong.crux.hbase/->kv-store
                   :hbase-connection  :hbase-connection
                   :table             "tx-log"}}
}
```
### Module `io.kosong.crux.hbase/->hbase-config`
#### Parameters
- `properties` - string map to set up `HBaseConfiguration`
#### Return
- An instance of `org.apache.hbase.HBaseConfiguration`

### Module `io.kosong.crux.hbase/->hbase-connection`
#### Parameters
- `hbase-config` - An instance of `HBaseConfiguration`
#### Return
- An instance of `org.apache.hbase.client.Connection`

### Module `io.kosong.crux.hbase/->kv-store`
#### Parameters
- `hbase-connection` - An instance of `org.apache.hbase.client.Connection`
- `table` - HBase table name of the KV Store.
- `namespace` - HBase namespace, default to `crux`.
- `family` - HBase column family name, default to `cf`.
- `qualifier` - HBase qualifier, default to `val`.
- `create-table?` - Create the HBase table if it doesn't exist, default to `true`.
#### Return
- An instance of `crux.kv.KvStore` with HBase table data store.

### Module `io.kosong.crux.hbase/->curator`
#### Parameters
- `zookeeper-quorum` - Zookeeper connection string
- `session-timeout` - Zookeeper session timeout in milliseconds, default to 60000.
- `connection-timeout` - Zookeeper connection timeout in milliseconds, default to 60000.
- `retry-base-sleep` - Exponential backoff retry base sleep in milliseconds, default to 2000.
- `retry-max-count` - Exponential backoff retry max count, default to 10.
#### Return
- An instance of `CuratorFramework`l


### Module `io.kosong.crux.hbase/->tx-ingest-executor`
#### Parameters
- `curator` - An instance of `CuratorFramework`.
- `tx-ingester` - Default to `:crux/tx-ingester`.
- `tx-log` - Default to `:crux/tx-log`.
- `index-store` - Default to `:crux/index-store`.
- `mutex-path` - Zookeeper leader election mutex path, defaults to `/crux/tx-ingest-executor`
#### Return
- An instance of `io.kosong.hbase.TxIngestExector`.



## Development

To build
``` shell script
lein modules install
```

To test
``` shell script
lein modules test
```

To run a Clojure REPL for development
```shell script
lein modules install
lein repl
```
When in Clojure REPL `user` namespace

```clojure
; Switch to dev namespace
(dev)

; If required, start embedded Zookeeper, HBase cluster
; for local development
(start-embedded-cluster)

; Start Crux
(go)

(crux/status (crux-node))

; Stop Crux
(halt)

; Stop embeded Zookeeper, HBase cluster
(stop-embedded-cluster)
```
