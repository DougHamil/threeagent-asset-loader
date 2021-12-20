(ns threeagent.assets-test
  (:require [threeagent.assets :as sut]
            [cljs.test :refer [async]
                       :refer-macros [deftest is testing]]))


(deftest model-load-test
  (testing "loading model file"
    (async done
           (let [db (atom {})]
             (->
              (sut/load! db [["/assets"
                              ["/models" {:loader sut/model-loader}
                               ["alien.glb" :model/alien {}]]]])
              (.then (fn []
                       (is (some? (:model/alien @db)))
                       (done)))
              (.catch (fn [err]
                        (is (nil? err))
                        (done))))))))
      


