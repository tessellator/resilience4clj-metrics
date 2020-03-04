(ns resilience4clj.circuit-breaker.metrics
  (:refer-clojure :exclude [name])
  (:require [clojure.string :as str]
            [metrics.core :as m])
  (:import [com.codahale.metrics
            Gauge
            MetricFilter
            MetricRegistry]
           [io.github.resilience4j.circuitbreaker
            CircuitBreaker
            CircuitBreakerRegistry]
           [io.github.resilience4j.circuitbreaker.utils
            MetricNames]
           [io.github.resilience4j.metrics
            CircuitBreakerMetrics]))

(set! *warn-on-reflection* true)

(defmulti ^:private register (fn [obj _ _] (class obj)))

(defn- ^String metric-name [part & parts]
  (MetricRegistry/name
   ^String part
   #^"[Ljava.lang.String;" (into-array String parts)))

(defn- gauge [f]
  (reify Gauge
    (getValue [_]
      (f))))

(defmethod register CircuitBreaker
  [^CircuitBreaker circuit-breaker ^String prefix ^MetricRegistry metric-registry]
  (let [name (.getName circuit-breaker)]
    (.register metric-registry
               (metric-name prefix name MetricNames/BUFFERED)
               (gauge #(.. circuit-breaker getMetrics getNumberOfBufferedCalls)))
    (.register metric-registry
               (metric-name prefix name MetricNames/FAILED)
               (gauge #(.. circuit-breaker getMetrics getNumberOfFailedCalls)))
    (.register metric-registry
               (metric-name prefix name MetricNames/FAILURE_RATE)
               (gauge #(.. circuit-breaker getMetrics getFailureRate)))
    (.register metric-registry
               (metric-name prefix name MetricNames/NOT_PERMITTED)
               (gauge #(.. circuit-breaker getMetrics getNumberOfNotPermittedCalls)))
    (.register metric-registry
               (metric-name prefix name MetricNames/SLOW)
               (gauge #(.. circuit-breaker getMetrics getNumberOfSlowCalls)))
    (.register metric-registry
               (metric-name prefix name MetricNames/SLOW_CALL_RATE)
               (gauge #(.. circuit-breaker getMetrics getSlowCallRate)))
    (.register metric-registry
               (metric-name prefix name MetricNames/SLOW_FAILED)
               (gauge #(.. circuit-breaker getMetrics getNumberOfSlowFailedCalls)))
    (.register metric-registry
               (metric-name prefix name MetricNames/SLOW_SUCCESS)
               (gauge #(.. circuit-breaker getMetrics getNumberOfSlowSuccessfulCalls)))
    (.register metric-registry
               (metric-name prefix name MetricNames/STATE)
               (gauge #(.. circuit-breaker getState getOrder)))
    (.register metric-registry
               (metric-name prefix name MetricNames/SUCCESSFUL)
               (gauge #(.. circuit-breaker getMetrics getNumberOfSuccessfulCalls)))
    nil))

(defmethod register CircuitBreakerRegistry
  [^CircuitBreakerRegistry registry prefix ^MetricRegistry metric-registry]
  (CircuitBreakerMetrics/ofCircuitBreakerRegistry prefix registry metric-registry))

(defn register!
  ([circuit-breaker-or-registry]
   (register! circuit-breaker-or-registry {}))
  ([circuit-breaker-or-registry opts]
   (let [{:keys [prefix metric-registry]
          :or {prefix MetricNames/DEFAULT_PREFIX
               metric-registry m/default-registry}} opts]
     (register circuit-breaker-or-registry prefix metric-registry))))

(defn- new-metric-filter [& parts]
  (let [start (str/join "." parts)]
   (reify MetricFilter
     (matches [_this name _metric]
       (str/starts-with? name start)))))

(defn- unregister-circuit-breaker
  [^CircuitBreaker circuit-breaker prefix ^MetricRegistry metric-registry]
  (let [^MetricFilter metric-filter (new-metric-filter prefix (.getName circuit-breaker))]
    (.removeMatching metric-registry metric-filter)))

(defmulti ^:private unregister (fn [obj _ _] (class obj)))

(defmethod unregister CircuitBreaker
  [^CircuitBreaker circuit-breaker prefix ^MetricRegistry metric-registry]
  (unregister-circuit-breaker circuit-breaker prefix metric-registry))

(defmethod unregister CircuitBreakerRegistry
  [^CircuitBreakerRegistry registry prefix ^MetricRegistry metric-registry]
  (doall (map #(unregister-circuit-breaker % prefix metric-registry)
              (.getAllCircuitBreakers registry))))

(defn unregister!
  ([circuit-breaker-or-registry]
   (unregister! circuit-breaker-or-registry {}))
  ([circuit-breaker-or-registry opts]
   (let [{:keys [prefix metric-registry]
          :or {prefix MetricNames/DEFAULT_PREFIX
               metric-registry m/default-registry}} opts]
     (unregister circuit-breaker-or-registry prefix metric-registry))
   nil))
