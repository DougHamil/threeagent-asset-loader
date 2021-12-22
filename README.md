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
        ["main_menu.ogg" :music/-main-menu {}]]]]])
        
(defn load-assets!
 "Loads all of the asset files, called during app initialization.
  Returns a Promise that resolves when all assets have been loaded"
  []
  (assets/load! asset-db asset-tree))
```

