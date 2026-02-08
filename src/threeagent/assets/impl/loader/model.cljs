(ns threeagent.assets.impl.loader.model
  (:require ["three/addons/loaders/GLTFLoader.js" :refer [GLTFLoader]]
            ["three/addons/loaders/FBXLoader.js" :refer [FBXLoader]]
            ["three/webgpu" :as three]
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

(defn- apply-shadow! [cast-shadow receive-shadow ^three/Object3D obj]
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
    ;; Preserve animation clips from GLTF result on the scene root
    ;; so they survive pool cloning (pool copies source.animations to clones)
    (when-let [anims (.-animations model)]
      (when (pos? (.-length anims))
        (set! (.-animations model-root) anims)))
    (res (preprocess! model-root cfg))))
                    
(defn loader [_key path cfg]
  (js/Promise. (fn [res rej]
                 (.load (select-loader path) path
                        #(on-load res cfg %)
                        nil
                        #(rej %)))))

