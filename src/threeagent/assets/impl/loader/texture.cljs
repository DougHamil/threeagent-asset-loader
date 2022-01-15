(ns threeagent.assets.impl.loader.texture
  (:require ["three" :as three]
            [threeagent.assets.impl.util :refer [set-props-camel-case!]]))

(def ^:private three-texture-loader (delay (three/TextureLoader.)))

(defn- on-load [res cfg ^three/Texture texture]
  (set-props-camel-case! texture cfg true)
  (res texture))

(defn loader [_key path cfg]
  (js/Promise. (fn [res rej]
                  (.load @three-texture-loader path
                         #(on-load res cfg %)
                         nil
                         rej))))

