# crux-hbase
[Crux](https://opencrux.com) key value store implementation with [HBase](https://hbase.apache.org).

## Development

- Build
``` shell script
lein modules install
```

- Testing
``` shell script
lein build
```

- To run a Clojure REPL for development
```shell script
lein modules install
lein repl
```

- When in Clojure REPL `user` namespace

```clojure
; Start embedded Zookeeper, HBase local cluster and Crux
(go)

(crux/status (crux-node))

; Stop cluster
(halt)
```