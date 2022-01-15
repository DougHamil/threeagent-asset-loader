(ns threeagent.assets.impl.loader.font-troika
  (:require ["troika-three-text" :as troika]))

(defn loader [_key path {:keys [characters]}]
  ;; TODO: Load error handling
  (js/Promise. (fn [res _rej]
                 (if characters
                   (troika/preloadFont (clj->js {:font path
                                                 :characters characters})
                                       #(res path))
                   (res path)))))
