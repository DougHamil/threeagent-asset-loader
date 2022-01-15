# Threeagent Asset Loader

This library can be used to load ThreeJS assets (models, textures, audio, etc) and store them in an atom.

## Installation

1. Add a dependency on this library to your `shadow-cljs.edn`
2. Install transient JS dependencies via `npm`:
```
npm install -S troika-three-text howler three
```

## Usage

To use this library, we need to define our "asset tree" which mirrors the layout of the directory structure where our
asset files are served. This makes it easy to keep our files organized on disk and update our application whenever
we need to load a new asset file.

For example, let's say our assets are under a directory structure like this:
```
- assets
  - models
    - alien.glb
    - robot.glb
  - textures
    - tile.png
    - particle_soft.png
  - audio
    - sfx
      - menu_accept.ogg
      - menu_decline.ogg
    - music
      - main_menu.ogg
```

In our application, we'd define our asset tree and load it like this:
```clojure
(ns my.app
  (:require [threeagent.assets :as assets]))
  
(defonce asset-db (atom {}))

(def asset-tree
  [["assets"
    ["models" {:loader assets/model-loader}
      ["alien.glb" :model/alien {}]
      ["robot.glb" :model/robot {}]]
    ["textures" {:loader assets/texture-loader}
      ["tile.png" :texture/tile {}]
      ["particle_soft.png" :texture/particle-soft {}]]
    ["audio" {:loader assets/audio-howler-loader} ;; Uses Howler.js to load audio
      ["sfx"
        ["menu_accept.ogg" :sfx/menu-accept {}]
        ["menu_decline.ogg" :sfx/menu-decline {}]]
      ["music"
        ["main_menu.ogg" :music/main-menu {}]]]]])
        
(defn load-assets!
 "Loads all of the asset files, called during app initialization.
  Returns a Promise that resolves when all assets have been loaded"
  []
  (assets/load! asset-db asset-tree))
```

After the promise returned from `assets/load!` completes, we can fetch assets from our `asset-db` using its key:
```clojure
(ns my.app.scene
  (:require [my.app :refer [asset-db]]))

(defn some-threeagent-component []
  [:object
   [:box {:material {:map (:texture/tile @asset-db)}}]])
```

## Loaders

This library comes with loaders for common types of assets: models, textures, audio, and fonts. These loaders are
wrappers around the standard ThreeJS loaders.

### model-loader

The `threeagent.assets/model-loader` is used to load 3D models. It currently supports GLTF and FBX files using the `GLTFLoader` and `FBXLoader` provided by ThreeJS.

Optionally, this loader can create a pool for each loaded model. This is useful when you need to add multiple copies of a model to your scene.

Example:
```clojure
  [["assets"
    ["models" {:loader assets/model-loader}
      ;; Load our alien.glb file, and create a pool with 5 copies of the model:
      ["alien.glb" :model/alien {:pool-size 5}]

      ;; Load our robot.fbx file, and set the scale of the loaded model to [10, 10, 10]:
      ["robot.fbx" :model/robot {:scale 10}]]]]
```

#### Pooling
When you define a `:pool-size`, you must use the provided `threeagent.assets.pool` namespace functions to claim/return models from/to the pool.

For example:
```clojure
(ns my.app.scene
  (:require [my.app :refer [asset-db]]
            [threeagent.assets.pool :as pool]))

(defn my-component []
  (let [model-pool (:model/alien @asset-db)
        model (pool/claim! model-pool)]
    [:object
      ^{:on-removed #(pool/return! model-pool model)} ;; Returns the model to the pool when this object is removed from the scene
      [:instance {:object model}]]))
```

It is recommended to define a custom Threeagent `IEntityType` specifically for dealing with pooled models. For example:

```clojure
(ns my-app.model-entity-type
  (:require [my.app :refer [asset-db]]
            [threeagent.assets.pool :as pool]
            [threeagent.entity :refer [IEntityType]]))

(deftype ModelEntity []
  IEntityType
  (create [_ _ {:keys [model-key]}]
    (let [model-pool (get @asset-db model-key)
          model (pool/claim! model-pool)]
      model))
  (destroy! [_ _ ^three/Object3D obj {:keys [model-key]}]
    (let [model-pool (get @asset-db model-key)]
      (pool/return! model-pool obj))))

```

### audio-howler-loader

The `threeagent.assets/audio-howler-loader` is used to load audio files as [Howler.js](https://github.com/goldfire/howler.js#documentation) `Howl` instances.

We can define the [options](https://github.com/goldfire/howler.js#options) used to construct the `Howl` instance via the asset properties map. For example:

```clojure
["audio" {:loader assets/audio-howler-loader}
  ["impacts.ogg" :sfx/impacts {:sprite {"1" [0 500] ;; Defined as [offset duration]
                                        "2" [500 200]}
                               :volume 0.2}]
  ["music.ogg" :music/main-menu {:loop true}]]

;; Usage
;; -- play music
(.play (:music/main-menu @asset-db))
;; -- play sprite
(.play (:sfx/impacts @asset-db) "1")
```

### font-troika-loader

The `threeagent.assets/font-troika-loader` is used to [preload fonts](https://github.com/protectwise/troika/tree/master/packages/troika-three-text) for usage with the [troika-three-text](https://github.com/protectwise/troika/tree/master/packages/troika-three-text) library. 

The loaded value will be the font file's path, which can be set as the `font` property on a Troika `Text` instance:

```clojure
["fonts" {:loader assets/font-troika-loader}
  ["menu_font.ttf" :font/main-menu {:characters ["a" "b" "c" "d" "1" "2" "3"]}]]
  
;; Usage
(let [text (troika/Text.)
      font (:font/main-menu @asset-db)]
  (set! (.-font text) font)
  (set! (.-text text) "abc")
  (.sync text)
  (.add my-threejs-scene text))
```

### texture-loader

The `threeagent.assets/texture-loader` is used to load [ThreeJS Textures](https://threejs.org/docs/#api/en/textures/Texture).
It uses the default [ThreeJS TextureLoader](https://threejs.org/docs/#api/en/loaders/TextureLoader) to load the texture file.

We can configure the loaded `Texture` instance using the configuration map:
```clojure
["textures" {:loader assets/texture-loader}
  ["grid.png" :texture/grid {:repeat {:x 4
                                      :y 4}
                             :rotation 0.4
                             :wrap-s three/RepeatWrapping
                             :wrap-t three/RepeatWrapping
                             :premultiply-alpha true}]]
```
