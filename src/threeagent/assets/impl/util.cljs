(ns threeagent.assets.impl.util
  (:require [camel-snake-kebab.core :as csk]))

(defn set-props-camel-case! [^js obj property-map]
  (doseq [[prop val] property-map]
    (let [prop-name (name prop)]
      (aset obj (csk/->camelCase prop-name) val)))
  obj)
