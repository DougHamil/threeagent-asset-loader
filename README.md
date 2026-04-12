# Threeagent Asset Loader
[![CircleCI](https://circleci.com/gh/DougHamil/threeagent-asset-loader/tree/main.svg?style=svg)](https://circleci.com/gh/DougHamil/threeagent-asset-loader/tree/main)
[![cljdoc badge](https://cljdoc.org/badge/com.github.doughamil/threeagent-asset-loader)](https://cljdoc.org/d/com.github.doughamil/threeagent-asset-loader)

This library can be used to load ThreeJS assets (models, textures, audio, etc) and store them in an atom.

## Installation
[![Clojars Project](https://img.shields.io/clojars/v/com.github.doughamil/threeagent-asset-loader.svg)](https://clojars.org/com.github.doughamil/threeagent-asset-loader)

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

## Loading from a Zip File

For faster loading, you can bundle your assets into a zip file and load them with `assets/load-zip!`. This downloads a single zip file, extracts it in memory, and loads assets from the extracted contents.

```clojure
(ns my.app
  (:require [threeagent.assets :as assets]))

(defonce asset-db (atom {}))

;; The asset tree paths should match the structure inside the zip
(def asset-tree
  [["models" {:loader assets/model-loader}
    ["alien.glb" :model/alien {}]
    ["robot.glb" :model/robot {}]]
   ["textures" {:loader assets/texture-loader}
    ["tile.png" :texture/tile {}]]])

(defn load-assets! []
  (assets/load-zip! asset-db "./assets.zip" asset-tree))
```

If your zip file has a root folder (e.g., the zip contains `assets/models/alien.glb` instead of `models/alien.glb`), use the `:base-path` option:

```clojure
(assets/load-zip! asset-db "./assets.zip" asset-tree {:base-path "assets"})
```

### Progress callbacks

`load-zip!` accepts two independent progress callbacks, covering two different phases:

```clojure
(assets/load-zip! asset-db "./assets.zip" asset-tree
  {:on-download-progress (fn [bytes-loaded bytes-total]
                           ;; fires while the zip is downloading
                           )
   :on-progress (fn [loaded total]
                  ;; fires once per asset after it is loaded from the extracted zip
                  )})
```

| Callback | Phase | Units |
| --- | --- | --- |
| `:on-download-progress` | Fetching the zip over the network | Bytes |
| `:on-progress` | Parsing each asset after the zip is extracted | Assets (items) |

For a large zip, the download phase typically dominates wall-clock time — so `:on-download-progress` is what you'll want for a realistic loading bar.

#### `:on-download-progress` details

Signature: `(fn [bytes-loaded bytes-total])`

- `bytes-loaded` — total bytes received so far. Monotonically non-decreasing.
- `bytes-total` — total bytes expected, parsed from the `Content-Length` response header. **`nil` when unknown** (see caveats below).

The callback is guaranteed to fire **at least once at completion** with `bytes-loaded == bytes-total` (both equal to the final received size), so callers can unconditionally flip a progress bar to 100% from the last call — even when the total was unknown during streaming.

**Caveats:**
- When the server omits `Content-Length` (or uses `Transfer-Encoding: chunked`), `bytes-total` will be `nil` during streaming. Render an indeterminate bar or show just the byte count in that case.
- With `Content-Encoding: gzip`, the header reports compressed size while the bytes you see are decompressed — so intermediate progress may overshoot 100%. The completion call always corrects this with `(final, final)`.
- For cross-origin fetches, the server must expose `Content-Length` via `Access-Control-Expose-Headers` for it to be visible to JavaScript.

When `:on-download-progress` is not provided, the library uses a faster non-streaming fetch — there is no overhead if you don't need the callback.

#### `:on-progress` details

Signature: `(fn [loaded total])` — available on both `load!` and `load-zip!`. Fires once per asset after it has been fully loaded and middleware has run. Each asset counts as 1 regardless of size.

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

### data-loader

The `threeagent.assets/data-loader` is used to load data files (JSON and EDN) and parse them as Clojure data structures.

File type is detected by extension:
- `.json` files are parsed with `JSON.parse` and converted to Clojure data
- `.edn` files are parsed with `clojure.edn/read-string`

By default, all string keys in maps are converted to keywords. This can be disabled with the `:keywordize-keys` option:

```clojure
["data" {:loader assets/data-loader}
  ;; JSON file - keys will be keywordized by default
  ["config.json" :data/config {}]

  ;; EDN file
  ["levels.edn" :data/levels {}]

  ;; Keep string keys (don't keywordize)
  ["external.json" :data/external {:keywordize-keys false}]]

;; Usage
(let [config (:data/config @asset-db)]
  (println (:name config))
  (println (get-in config [:settings :debug])))
```

## Middleware

Middleware lets you post-process an asset's loaded value before it's stored in the database. Useful for attaching derived data (bounding boxes, collision shapes), tagging, or normalizing values without forking a loader.

A middleware is a plain function:

```clojure
(fn [key data resolved-config] -> new-data)
```

- `key` — the asset's keyword
- `data` — the loader's output (or the previous middleware's return value)
- `resolved-config` — the leaf's config map, with any `(assets/ref :other)` entries already substituted with the loaded asset

The return value becomes the input to the next middleware in the chain, and the final value is what lands in the asset database.

### Attaching middleware

Middleware can be attached at two levels.

**Branch-scoped** — applies to every leaf beneath the branch:

```clojure
["models" {:loader assets/model-loader
           :middleware [tag-with-category-mw]}
  ["alien.glb" :model/alien {}]
  ["robot.glb" :model/robot {}]]
```

**Leaf-scoped** — applies to one asset only:

```clojure
["models" {:loader assets/model-loader}
  ["crate.glb" :model/crate {:scale 2
                             :middleware [bounding-box-mw]}]
  ["hero.glb" :model/hero {}]]
```

You can mix both — a leaf sees its own middleware *and* all middleware declared on branches above it.

### Execution order

For each asset, the loader pipeline runs in this order:

1. Referenced assets (via `assets/ref`) are loaded first.
2. The loader runs and returns the raw `data`.
3. Middleware runs as a chain: **leaf middleware first, then branch middleware from innermost to outermost branch**. Each middleware receives the previous one's output.
4. The final value is stored in the asset database under the asset's key.

Given:

```clojure
["outer" {:loader my-loader :middleware [outer-mw]}
  ["inner" {:middleware [inner-mw]}
    ["thing.bin" :thing {:middleware [leaf-mw]}]]]
```

The chain for `:thing` runs as: `leaf-mw` → `inner-mw` → `outer-mw`.

### Example: bounding-box middleware

```clojure
(defn bounding-box-mw [_key model _config]
  (let [bbox (three/Box3.)]
    (.setFromObject bbox model)
    (set! (.. model -userData -boundingBox) bbox)
    model))

(def asset-tree
  [["models" {:loader assets/model-loader}
    ["crate.glb" :model/crate {:middleware [bounding-box-mw]}]
    ["hero.glb"  :model/hero {}]]])

;; usage, after load! resolves
(.. (:model/crate @asset-db) -userData -boundingBox)
```

### Notes

- Middleware is **synchronous**. Return a value, not a promise. If you need async work, write a custom loader instead.
- `resolved-config` has `(assets/ref …)` values substituted with the actual loaded assets, so middleware can read sibling assets directly.
- When a model uses `:pool-size`, the loader returns a pool atom (see [Pooling](#pooling)), not an `Object3D`. Middleware sees whatever the loader returned, so it must handle the pool shape if pooling is enabled.
- `:middleware` is stripped from the config before the loader sees it, so middleware never collides with loader config keys.

## Development

### Running Tests

```bash
npm ci
npm run test-once
```

For watch mode during development:
```bash
npm run watch-test
```

### Releasing

Releases are managed through CircleCI and deployed to Clojars.

1. Push to the `release` branch:
   ```bash
   git checkout -b release
   git push origin release
   ```

2. Wait for tests to pass in CircleCI

3. Approve the release in the CircleCI UI (there's a manual hold step)

4. The release job will automatically:
   - Bump the version from SNAPSHOT to release
   - Tag the release with `vX.X.X`
   - Deploy to Clojars
   - Bump to the next SNAPSHOT version
   - Merge back to `main`

Snapshots are automatically deployed to Clojars on every push to `main`.
