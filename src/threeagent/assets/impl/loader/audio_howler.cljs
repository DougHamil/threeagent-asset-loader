(ns threeagent.assets.impl.loader.audio-howler
  (:require ["howler" :refer [Howl]]))

(defn loader [_key path {:keys [volume]}]
  (js/Promise. (fn [res rej]
                 (let [howl (Howl. (clj->js {:src path
                                             :volume (or volume 1.0)
                                             :preload false}))]
                   (.once howl "load" #(res howl))
                   (.once howl "loaderror" (fn [_sound-id error]
                                             (rej error)))
                   (.load howl)))))
