(ns threeagent.assets.impl.loader.audio-howler
  (:require ["howler" :refer [Howl]]))

(defn loader [_key path cfg]
  (js/Promise. (fn [res rej]
                 (let [howl (Howl. (-> cfg
                                       (merge {:src path
                                               :preload false})
                                       (clj->js)))]
                   (.once howl "load" #(res howl))
                   (.once howl "loaderror" (fn [_sound-id error]
                                             (rej error)))
                   (.load howl)))))
