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
  <version>0.2.0-SNAPSHOT</version>
</dependency>

```
### Leiningen
```clojure
[io.kosong.crux.hbase/crux-hbase "0.2.0-SNAPSHOT"]
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

  :crux/index-store
    {:kv-store {:crux/module       'io.kosong.crux.hbase/->kv-store
                :hbase-connection  :hbase-connection
                :table             "index-store"}}

  :crux/document-store
    {:kv-store {:crux/module       'io.kosong.crux.hbase/->kv-store
                :hbase-connection  :hbase-connection
                :table             "document-store"}}

  :crux/tx-log
    {:kv-store {:crux/module       'io.kosong.crux.hbase/->kv-store
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
- `table` HBase table name of the KV Store.
- `namespace` the HBase namespace, default to `crux`.
- `family` HBase column family name, default to `cf`.
- `qualifier` HBase qualifier, default to `val`.
- `create-table?` Create the HBase table if it doesn't exist. default to `true`.
#### Return
- An instance of `crux.kv.KvStore` with HBase table data store.


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
