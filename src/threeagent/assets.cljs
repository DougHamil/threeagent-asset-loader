(ns threeagent.assets
  (:require [threeagent.assets.impl.core :as impl]
            [threeagent.assets.impl.loader.model :as model]
            [threeagent.assets.impl.loader.texture :as texture]
            [threeagent.assets.impl.loader.audio-howler :as audio-howler]
            [threeagent.assets.impl.loader.font-troika :as font-troika]))

(defn load!
  "Loads the assets defined in the `asset-tree` into the `asset-database` atom.

   Returns a Promise:
    * on success: all assets have been loaded into the `asset-database` 
    * on failure: one or more assets have failed to load. See the console for error messages"
  [asset-database asset-tree]
  (impl/load! asset-database asset-tree))

(def ref impl/ref)

(def model-loader
  "Loader for 3D models. Currently supports GLTF, GLB and FBX files."
  model/loader)

(def texture-loader
  "Loader for 2D textures."
  texture/loader)

(def audio-howler-loader
  "Loader for audio. Assets will be loaded as Howler.js `Howl` instances"
  audio-howler/loader)

(def font-troika-loader
  "Loader for `three-troika-text` fonts. Should be used to pre-load font files"
  font-troika/loader)
  
