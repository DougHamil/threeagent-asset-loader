(ns threeagent.assets.impl.loader.texture
  (:require ["three" :as three]
            [threeagent.assets.impl.util :as util]))

(def ^:private three-texture-loader (delay (three/TextureLoader.)))

(defn- on-load [res cfg ^three/Texture texture]
  (util/set-props-camel-case! texture cfg)
  (res texture))

(defn loader [_key path cfg]
  (js/Promise. (fn [res _rej]
                  (.load @three-texture-loader path
                         #(on-load res %)))))

