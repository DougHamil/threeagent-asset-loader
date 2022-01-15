(ns threeagent.assets.impl.loader.texture-test
  (:require [cljs.test :refer-macros [deftest is testing async]]
            [threeagent.assets.impl.loader.texture :as sut]))

(deftest load-error-test
  (testing "missing file produces error"
    (async done
           (-> (sut/loader :texture/missing "/assets/textures/missing.png" {})
               (.catch (fn [err]
                         (is (some? err))
                         (done)))))))

(deftest texture-properties-test
  (testing "Vector2 typed properties work"
    (async done
           (-> (sut/loader :texture/black "/assets/textures/black.png" {:repeat {:x 2
                                                                                 :y 2}})
               (.then (fn [texture]
                        (is (= 2 (.-x (.-repeat texture))))
                        (done)))
               (.catch (fn [err]
                         (is (nil? err))
                         (done)))))))
