(ns threeagent.assets
  (:require [threeagent.assets.impl.core :as impl]
            [threeagent.assets.impl.zip :as zip]
            [threeagent.assets.impl.loader.model :as model]
            [threeagent.assets.impl.loader.texture :as texture]
            [threeagent.assets.impl.loader.audio-howler :as audio-howler]
            [threeagent.assets.impl.loader.font-troika :as font-troika]
            [threeagent.assets.impl.loader.data :as data]))

(defn load!
  "Loads the assets defined in the `asset-tree` into the `asset-database` atom.

   Returns a Promise:
    * on success: all assets have been loaded into the `asset-database`
    * on failure: one or more assets have failed to load. See the console for error messages"
  [asset-database asset-tree]
  (impl/load! asset-database asset-tree))

(defn load-zip!
  "Loads assets from a zip file into the `asset-database` atom.

   The zip file is downloaded, extracted to memory, and assets are loaded
   from the extracted files using ObjectURLs.

   Options:
     :base-path - Path prefix inside the zip to strip. Use this if the zip
                  has a root folder (e.g. if zip contains assets/models/foo.glb
                  and your tree references models/foo.glb, set base-path to \"assets\")

   Returns a Promise:
    * on success: all assets have been loaded into the `asset-database`
    * on failure: one or more assets have failed to load. See the console for error messages

   Example:
     (load-zip! db \"./assets.zip\"
       [[\"models\" {:loader model-loader}
         [\"alien.glb\" :model/alien {}]]
        [\"textures\" {:loader texture-loader}
         [\"tile.png\" :texture/tile {}]]])"
  ([asset-database zip-url asset-tree]
   (load-zip! asset-database zip-url asset-tree {}))
  ([asset-database zip-url asset-tree {:keys [base-path]}]
   (zip/load-zip! asset-database zip-url asset-tree {:base-path (or base-path "")})))

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

(def data-loader
  "Loader for data files (JSON, EDN). Parses content as Clojure data structures.

   Options:
     :keywordize-keys - Convert string keys to keywords (default: true)"
  data/loader)

