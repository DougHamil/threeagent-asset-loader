(ns threeagent.assets-test
  (:require [threeagent.assets :as sut]
            [cljs.test :refer [async]
                       :refer-macros [deftest is]]))

(def ^:private asset-tree
  [["/assets"
    ["/models" {:loader sut/model-loader}
     ["alien.glb" :model/alien {}]]
    ["/textures" {:loader sut/texture-loader}
     ["black.png" :texture/black {}]]]])

(deftest loaders-test
  (async done
         (let [db (atom {})]
           (-> (sut/load! db asset-tree)
               (.then (fn []
                        (is (some? (:model/alien @db)))
                        (is (some? (:texture/black @db)))
                        (done)))
               (.catch (fn [error]
                         (is (nil? error))
                         (done)))))))
