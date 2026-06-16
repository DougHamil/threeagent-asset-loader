(ns threeagent.assets.impl.loader.hdri
  (:require ["three/examples/jsm/loaders/EXRLoader.js" :refer [EXRLoader]]
            ["three/examples/jsm/loaders/HDRLoader.js" :refer [HDRLoader]]
            [clojure.string :as string]))

(def ^:private exr-loader (delay (EXRLoader.)))
(def ^:private hdr-loader (delay (HDRLoader.)))

(defn- pick-loader
  "EXR vs Radiance-HDR (.hdr) by file extension. Defaults to EXR — a path
   may be a content-addressed URL with no extension."
  [path]
  (if (string/ends-with? (string/lower-case (or path "")) ".hdr")
    @hdr-loader
    @exr-loader))

(defn loader
  "Loads an EXR or Radiance-HDR (.hdr) file as an equirectangular texture,
   picking the three.js loader by file extension. Returns a
   Promise<Texture>. Use with PMREMGenerator post-load to create an
   environment map."
  [_key path _cfg]
  (js/Promise. (fn [res rej]
                 (.load (pick-loader path) path
                        (fn [texture]
                          (res texture))
                        nil
                        rej))))
