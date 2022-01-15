(ns threeagent.assets.impl.loader.audio-howler
  (:require ["howler" :refer [Howl]]))

(defn- on-error [path sound-id error]
  (js/console.error "Failed to load audio at path %s due to error:\n" error))

(defn loader [_key path {:keys [volume]}]
  ;; TODO: Wrap Howler loading in proper promise
  (let [error-cb (partial on-error path)]
    (js/Promise. (fn [res rej]
                     (res (Howl. (clj->js {:src path
                                           :volume (or volume 1.0)
                                           :onloaderror error-cb})))))))

