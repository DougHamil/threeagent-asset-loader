(ns threeagent.assets.impl.loader.model
  (:require ["three/examples/jsm/loaders/GLTFLoader" :refer [GLTFLoader]]
            ["three/examples/jsm/loaders/FBXLoader" :refer [FBXLoader]]
            ["three" :as three]
            [threeagent.assets.pool :as pool]))

(def ^:private gltf-loader (delay (GLTFLoader.)))
(def ^:private fbx-loader (delay (FBXLoader.)))

(def ^:private loaders-by-ext {#"(?i).+\.glb$" gltf-loader
                               #"(?i).+\.gltf$" gltf-loader
                               #"(?i).+\.fbx$" fbx-loader})

(defn select-loader [path]
  (->> loaders-by-ext
       (filter (fn [[regex _loader]]
                 (re-matches regex path)))
       (map second)
       (first)
       (deref)))

(defn- apply-scale! [scale ^three/Object3D obj]
  (.set (.-scale obj) scale scale scale)
  obj)

(defn- apply-shadow! [^three/Object3D obj cast-shadow receive-shadow]
  (.traverse obj (fn [^js obj]
                   (set! (.-castShadow obj) cast-shadow)
                   (set! (.-receiveShadow obj) receive-shadow)))
  obj)

(defn- preprocess! [^three/Object3D obj {:keys [scale pool-size cast-shadow receive-shadow]}]
  (cond->> obj
    true (apply-shadow! cast-shadow receive-shadow)
    scale (apply-scale! scale)
    pool-size (pool/create pool-size)))

(defn- on-load [res cfg ^three/Object3D model]
  (let [model-root (or (.-scene model) model)]
    (res (preprocess! model-root cfg))))
                    
(defn- on-error [path rej error]
  (js/console.error "Failed to load model at path %s due to error:\n" error)
  (rej [path error]))

(defn loader [_key path cfg]
  (js/Promise. (fn [res rej]
                 (.load (select-loader path) path
                        #(on-load res cfg %)
                        nil
                        #(on-error path rej %)))))

