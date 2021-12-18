(ns threeagent.assets
  (:require [threeagent.assets.impl.core :as impl]
            [threeagent.assets.impl.loader.model :as model]
            [threeagent.assets.impl.loader.texture :as texture]
            [threeagent.assets.impl.loader.audio-howler :as audio-howler]
            [threeagent.assets.impl.loader.font-troika :as font-troika]))

(defn load! [asset-database asset-tree]
  (impl/load! asset-database asset-tree))

(def model-loader model/loader)
(def texture-loader texture/loader)
(def audio-howler-loader audio-howler/loader)
(def font-troika-loader font-troika/loader)
  
