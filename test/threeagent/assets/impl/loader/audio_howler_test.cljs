(ns threeagent.assets.impl.loader.audio-howler-test
  (:require [cljs.test :refer-macros [deftest is testing async]]
            [threeagent.assets.impl.loader.audio-howler :as sut]))

(deftest load-error-test
  (testing "missing audio file produces error"
    (async done
           (-> (sut/loader :audio/missing "/assets/audio/missing.ogg" {})
               (.catch (fn [err]
                         (is (some? err))
                         (done)))))))
(deftest load-success-test
  (testing "valid audio file loads successfully"
    (async done
           (-> (sut/loader :audio/good "/assets/audio/good.ogg" {})
               (.then (fn [audio]
                        (is (some? audio))
                        (done)))
               (.catch (fn [err]
                         (is (nil? err))
                         (done)))))))

(deftest sprites-test
  (testing "audio sprite files load successfully"
    (async done
           (-> (sut/loader :audio/good "/assets/audio/good.ogg" {:sprite {"a" [0 100]
                                                                          "b" [100 100]}})
               (.then (fn [audio]
                        (is (some? audio))
                        (done)))
               (.catch (fn [err]
                         (is (nil? err))
                         (done)))))))
