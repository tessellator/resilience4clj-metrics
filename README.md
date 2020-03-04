# resilience4clj-metrics

A small Clojure wraper around the resilience4j metrics module.

Requires Clojure 1.8 or later for JDK 8, and Clojure 1.10 or later for JDK 9+.

[![clojars badge](https://img.shields.io/clojars/v/tessellator/resilience4clj-metrics.svg)](https://clojars.org/tessellator/resilience4clj-metrics)

## Quick Start

The following demonstrates how to register a circuit-breaker to report its
metrics to the default metric registry from the
[metrics-clojure library](https://github.com/metrics-clojure/metrics-clojure)
and using the prefix `my.namespace`.


```clojure
(require '[resilience4clj.circuit-breaker :as cb])
(require '[resilience4clj.circuit-breaker.metrics :as cbm])

(def my-breaker (cb/circuit-breaker :my-breaker))
(cbm/register! my-breaker {:prefix "my.namespace"})
```

## License

Copyright © 2020 Thomas C. Taylor and contributors.

Distributed under the Eclipse Public License version 2.0.
