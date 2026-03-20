(ns threeagent.assets.impl.zip
  (:require ["jszip" :as JSZip]
            [clojure.string :as string]
            [threeagent.assets.impl.core :as core]))

(def ^:private extension->mime-type
  {".glb"  "model/gltf-binary"
   ".gltf" "model/gltf+json"
   ".fbx"  "application/octet-stream"
   ".png"  "image/png"
   ".jpg"  "image/jpeg"
   ".jpeg" "image/jpeg"
   ".gif"  "image/gif"
   ".webp" "image/webp"
   ".ogg"  "audio/ogg"
   ".mp3"  "audio/mpeg"
   ".wav"  "audio/wav"
   ".ttf"  "font/ttf"
   ".otf"  "font/otf"
   ".woff" "font/woff"
   ".woff2" "font/woff2"
   ".json" "application/json"
   ".bin"  "application/octet-stream"})

(defn- get-extension [path]
  (let [idx (string/last-index-of path ".")]
    (when (and idx (pos? idx))
      (string/lower-case (subs path idx)))))

(defn- get-mime-type [path]
  (get extension->mime-type (get-extension path) "application/octet-stream"))

(defn- normalize-path
  "Strips leading ./ and / from path for consistent lookup."
  [path]
  (cond-> path
    (string/starts-with? path "./") (subs 2)
    (string/starts-with? path "/") (subs 1)))

(defn- fetch-zip [url]
  (-> (js/fetch url)
      (.then (fn [response]
               (if (.-ok response)
                 (.arrayBuffer response)
                 (throw (ex-info "Failed to fetch zip file"
                                 {:url url
                                  :status (.-status response)
                                  :statusText (.-statusText response)})))))
      (.catch (fn [err]
                (if (ex-data err)
                  (throw err)
                  (throw (ex-info "Failed to fetch zip file"
                                  {:url url
                                   :cause err})))))))

(defn- parse-zip [array-buffer]
  (-> (JSZip.)
      (.loadAsync array-buffer)
      (.catch (fn [err]
                (throw (ex-info "Failed to parse zip file"
                                {:cause err}))))))

(defn- extract-files
  "Extracts all files from the zip and returns a map of {path -> blob-url}."
  [zip base-path]
  (let [file-promises (transient [])]
    (.forEach zip (fn [relative-path entry]
                    (when-not (.-dir entry)
                      (let [normalized (normalize-path relative-path)
                            ;; Strip base-path prefix if present
                            lookup-path (if (and (seq base-path)
                                                 (string/starts-with? normalized (str base-path "/")))
                                          (subs normalized (inc (count base-path)))
                                          normalized)
                            mime-type (get-mime-type relative-path)]
                        (conj! file-promises
                               (-> (.async entry "blob")
                                   (.then (fn [blob]
                                            (let [typed-blob (js/Blob. #js [blob] #js {:type mime-type})
                                                  url (js/URL.createObjectURL typed-blob)]
                                              [lookup-path url])))))))))
    (-> (js/Promise.all (persistent! file-promises))
        (.then (fn [results]
                 (into {} results))))))

(defn- create-url-resolver
  "Creates a url-resolver function that maps asset paths to blob URLs.
   Appends original filename as URL hash fragment for loader type detection.
   Throws descriptive error for missing paths."
  [url-map available-paths base-path]
  (fn [path]
    (let [normalized (normalize-path path)
          ;; Strip base-path prefix to match the keys in url-map
          lookup-key (if (and (seq base-path)
                              (string/starts-with? normalized (str base-path "/")))
                       (subs normalized (inc (count base-path)))
                       normalized)]
      (if-let [url (get url-map lookup-key)]
        ;; Append original filename as hash fragment so loaders can detect file type
        ;; The hash fragment is preserved for regex matching but ignored when fetching
        (let [filename (last (string/split normalized #"/"))]
          (str url "#" filename))
        (throw (ex-info "Asset not found in zip file"
                        {:requested-path path
                         :normalized-path normalized
                         :lookup-key lookup-key
                         :available-paths available-paths}))))))

(defn- revoke-urls! [url-map]
  (doseq [[_ url] url-map]
    (js/URL.revokeObjectURL url)))

(defn load-zip!
  "Loads assets from a zip file into the asset database.

   Options:
     :base-path - Path prefix inside the zip to strip (e.g. \"assets\" if zip contains assets/models/foo.glb)

   Returns a Promise that resolves when all assets are loaded."
  [database zip-url asset-tree {:keys [base-path]}]
  (let [url-map-atom (atom nil)]
    (-> (fetch-zip zip-url)
        (.then parse-zip)
        (.then (fn [zip]
                 (extract-files zip (or base-path ""))))
        (.then (fn [url-map]
                 (reset! url-map-atom url-map)
                 (let [available-paths (vec (keys url-map))
                       url-resolver (create-url-resolver url-map available-paths (or base-path ""))]
                   (core/load! database asset-tree url-resolver))))
        (.then (fn [result]
                 (when-let [url-map @url-map-atom]
                   (revoke-urls! url-map))
                 result))
        (.catch (fn [err]
                  (when-let [url-map @url-map-atom]
                    (revoke-urls! url-map))
                  (throw err))))))
