(ns threeagent.assets.impl.loader.model
  (:require ["three/addons/loaders/GLTFLoader.js" :refer [GLTFLoader]]
            ["three/addons/loaders/FBXLoader.js" :refer [FBXLoader]]
            ["three/webgpu" :as three]
            [clojure.string :as string]
            [threeagent.assets.pool :as pool]))

(def ^:private default-gltf-loader (delay (GLTFLoader.)))
(def ^:private default-fbx-loader (delay (FBXLoader.)))

(defn- select-default-loader [path]
  (cond
    (re-matches #"(?i).+\.glb$" path) @default-gltf-loader
    (re-matches #"(?i).+\.gltf$" path) @default-gltf-loader
    (re-matches #"(?i).+\.fbx$" path) @default-fbx-loader
    :else nil))

(defn- parent-dir
  "Get the parent directory of a path (e.g., 'models/foo.glb' -> 'models/')."
  [path]
  (let [idx (string/last-index-of path "/")]
    (if idx
      (subs path 0 (inc idx))
      "")))

(defn- create-loader-with-manager [path resolve-url original-path]
  (let [;; Base directory of the original asset path in the zip
        base-dir (parent-dir original-path)
        manager (three/LoadingManager.)
        ;; The blob origin prefix (e.g., "blob:http://localhost:8080/")
        ;; used to extract relative paths from URLs that Three.js resolved
        ;; against the blob URL origin
        blob-origin (let [model-blob (subs path 0 (string/last-index-of path "/"))]
                      (str model-blob "/"))
        _ (.setURLModifier manager
            (fn [url]
              (cond
                ;; Real blob URL (the model itself or an embedded buffer) — pass through
                ;; Real blob URLs have a UUID segment, not a file path
                (and (string/starts-with? url "blob:")
                     (not (re-find #"\.\w+$" url)))
                url

                ;; Mangled blob URL: Three.js resolved a relative path against
                ;; the blob origin (e.g., "blob:http://host/Textures/foo.png")
                ;; Extract the relative part and resolve through the zip
                (string/starts-with? url blob-origin)
                (let [relative (subs url (count blob-origin))
                      zip-path (str base-dir relative)]
                  (try
                    (resolve-url zip-path)
                    (catch :default _
                      url)))

                ;; Plain relative path — prepend base dir and resolve
                :else
                (let [zip-path (str base-dir url)]
                  (try
                    (resolve-url zip-path)
                    (catch :default _
                      url))))))]
    (cond
      (re-matches #"(?i).+\.glb$" path) (GLTFLoader. manager)
      (re-matches #"(?i).+\.gltf$" path) (GLTFLoader. manager)
      (re-matches #"(?i).+\.fbx$" path) (FBXLoader. manager)
      :else nil)))

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
                    
(defn loader
  ([_key path cfg] (loader _key path cfg nil nil))
  ([_key path cfg resolve-url original-path]
   (let [ldr (if resolve-url
               (create-loader-with-manager path resolve-url original-path)
               (select-default-loader path))]
     (js/Promise. (fn [res rej]
                    (.load ldr path
                           #(on-load res cfg %)
                           nil
                           #(rej %)))))))

