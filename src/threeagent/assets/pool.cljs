(ns threeagent.assets.pool
  (:require ["three/examples/jsm/utils/SkeletonUtils" :as SkeletonUtils]))

(defn create [count source]
  (let [pool (atom {})]
    (doseq [_ (range count)]
      (let [clone (SkeletonUtils/clone source)
            _ (set! (.-animations clone) (.-animations source))]
        (swap! pool assoc (.-uuid clone) clone)))
    pool))

(defn claim! [pool]
  (when-let [[item-id item] (first @pool)]
    (swap! pool dissoc item-id)
    item))

(defn return! [pool item]
  (swap! pool assoc (.-uuid item) item))

(defn size [pool]
  (count @pool))

