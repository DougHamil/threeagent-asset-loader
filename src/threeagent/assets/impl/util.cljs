(ns threeagent.assets.impl.util
  (:require [camel-snake-kebab.core :as csk]
            ["three" :as three]))

(defn- ->vector2 [o]
  (cond
    (map? o) (three/Vector2. (:x o)
                             (:y o))
    (seq? o) (three/Vector2. (first o) (second o))
    :else o))

(defn- ->vector3 [o]
  (cond
    (map? o) (three/Vector3. (:x o)
                             (:y o)
                             (:z o))
    (seq? o) (three/Vector3. (first o)
                             (second o)
                             (nth o 2))
    :else o))

(defn- coerce-prop [^js ref-val prop-val]
  (cond
    (instance? three/Vector2 ref-val) (->vector2 prop-val)
    (instance? three/Vector3 ref-val) (->vector3 prop-val)
    :else prop-val))

(defn- coerce-prop-map [^js obj property-map]
  (reduce-kv
   (fn [acc k v]
     (if-let [ref-val (aget obj (csk/->camelCase (name k)))]
       (assoc acc k (coerce-prop ref-val v))
       (assoc acc k v)))
   {}
   property-map))

(defn set-props-camel-case!
  ([^js obj property-map]
   (set-props-camel-case! obj property-map false))
  ([^js obj property-map coerce?]
   (let [property-map (cond->> property-map
                        coerce? (coerce-prop-map obj))]
     (doseq [[prop val] property-map]
       (let [prop-name (name prop)]
         (aset obj (csk/->camelCase prop-name) (clj->js val)))))
   obj))

