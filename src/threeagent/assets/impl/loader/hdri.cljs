(ns threeagent.assets.impl.loader.hdri
  (:require ["three/examples/jsm/loaders/EXRLoader.js" :refer [EXRLoader]]))

(def ^:private exr-loader (delay (EXRLoader.)))

(defn loader
  "Loads an EXR/HDR file as an equirectangular texture.
   Returns a Promise<Texture>. Use with PMREMGenerator post-load
   to create an environment map."
  [_key path _cfg]
  (js/Promise. (fn [res rej]
                 (.load @exr-loader path
                        (fn [texture]
                          (res texture))
                        nil
                        rej))))
