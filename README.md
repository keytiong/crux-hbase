# crux-hbase
This is a [Crux](https://opencrux.com) key value store implementation with
[HBase](https://hbase.apache.org).

## Usage

Maven
```xml
<dependency>
  <groupId>io.kosong.crux</groupId>
  <artifactId>crux-hbase</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>

```
Leiningen
```clojure
[io.kosong.crux.hbase/crux-hbase "0.1.0-SNAPSHOT"]
```


## Module Configuration

Refer to Crux [reference](https://opencrux.com/reference/installation.html) for
details on how to configure Crux index store, document store and transaction log.

The first step is to specify the configuration for the HBase connection with
`io.kosong.crux.hbase/->hbase-connection` module.

And then we can use the `io.kosong.crux.hbase/->kv-store` module to configure
the desired Crux components (index store, document store or transaction log)
to use HBase KV Store.

Below is an example of configuring Crux to use HBase KV Stores for all three major
components (index store, document store and transaction log).

```clojure
{
  ;; create a shared hbase connection
  :hbase-connection
    {:crux/module  'io.kosong.crux.hbase/->hbase-connection
     :hbase-config {"hbase.zookeeper.quorum" "127.0.0.1:2181"}}

  :crux/index-store
    {:kv-store {:crux/module 'io.kosong.crux.hbase/->kv-store
                :connection  :hbase-connection
                :table       "index-store"}}

  :crux/document-store
    {:kv-store {:crux/module 'io.kosong.crux.hbase/->kv-store
                :connection  :hbase-connection
                :table       "document-store"}}

  :crux/tx-log
    {:kv-store {:crux/module 'io.kosong.crux.hbase/->kv-store
                :connection  :hbase-connection
                :table       "tx-log"}}
}
```
### Module `io.kosong.crux.hbase/->hbase-onnection` Parameters
- `hbase-config` string map to set up `HBaseConfiguration` for the HBase
   client connection

### Module `io.kosong.crux.hbase/->kv-store` Parameters
- `connection` the HBase connection module
- `table` the HBase table name of the KV Store



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

; Start embedded Zookeeper, HBase local cluster and Crux
(go)

(crux/status (crux-node))

; Stop cluster
(halt)
```
