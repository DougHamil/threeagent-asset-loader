(ns threeagent.assets-test
  (:require [threeagent.assets :as sut]
            [cljs.test :refer [async]
                       :refer-macros [deftest is]]))

(def ^:private asset-tree
  [["/assets"
    ["/models" {:loader sut/model-loader}
     ["alien.glb" :model/alien {}]]
    ["/textures" {:loader sut/texture-loader}
     ["black.png" :texture/black {}]]
    ["/audio" {:loader sut/audio-howler-loader}
     ["good.ogg" :audio/good {}]]]])

(deftest loaders-test
  (async done
         (let [db (atom {})]
           (-> (sut/load! db asset-tree)
               (.then (fn []
                        (is (some? (:model/alien @db)))
                        (is (some? (:texture/black @db)))
                        (is (some? (:audio/good @db)))
                        (done)))
               (.catch (fn [error]
                         (is (nil? error))
                         (done)))))))
