(ns threeagent.assets.impl.loader.font-troika
  (:require ["troika-three-text" :as troika]))

(defn loader [_key path {:keys [characters]}]
  ;; TODO: Load error handling
  (js/Promise. (fn [res _rej]
                 (troika/preloadFont (clj->js {:font path
                                               :characters characters})
                                     (fn []
                                       ;; Force shader to compile
                                       (let [text (troika/Text.)]
                                         (.sync text #(res path))))))))
  

