(ns threeagent.assets.impl.zip-test
  (:require [threeagent.assets :as sut]
            [cljs.test :refer [async]
                       :refer-macros [deftest is testing]]))

(def ^:private zip-asset-tree
  [["models" {:loader sut/model-loader}
    ["alien.glb" :model/alien {}]]
   ["textures" {:loader sut/texture-loader}
    ["black.png" :texture/black {}]]
   ["audio" {:loader sut/audio-howler-loader}
    ["good.ogg" :audio/good {}]]
   ["data" {:loader sut/data-loader}
    ["config.json" :data/config {}]]])

(deftest load-zip-test
  (async done
         (let [db (atom {})]
           (-> (sut/load-zip! db "/base/test-assets.zip" zip-asset-tree)
               (.then (fn []
                        (is (some? (:model/alien @db)) "Model should be loaded from zip")
                        (is (some? (:texture/black @db)) "Texture should be loaded from zip")
                        (is (some? (:audio/good @db)) "Audio should be loaded from zip")
                        (is (some? (:data/config @db)) "Data should be loaded from zip")
                        (is (= "zip-config" (:name (:data/config @db))) "Data content should be parsed correctly")
                        (is (= true (:enabled (:data/config @db))) "Data keys should be keywordized")
                        (done)))
               (.catch (fn [error]
                         (js/console.error "load-zip-test failed:" error)
                         (is (nil? error) "Should not fail to load from zip")
                         (done)))))))

(deftest load-zip-missing-file-test
  (testing "Throws error when asset path not found in zip"
    (async done
           (let [db (atom {})
                 bad-tree [["nonexistent" {:loader sut/model-loader}
                            ["missing.glb" :model/missing {}]]]]
             (-> (sut/load-zip! db "/base/test-assets.zip" bad-tree)
                 (.then (fn []
                          (is false "Should have thrown an error")
                          (done)))
                 (.catch (fn [error]
                           (is (some? error) "Should throw error for missing asset")
                           (let [data (ex-data error)]
                             (is (= "nonexistent/missing.glb" (:normalized-path data))
                                 "Error should include normalized path")
                             (is (vector? (:available-paths data))
                                 "Error should include available paths"))
                           (done))))))))

(deftest load-zip-bad-url-test
  (testing "Throws error when zip URL is invalid"
    (async done
           (let [db (atom {})]
             (-> (sut/load-zip! db "/base/nonexistent.zip" zip-asset-tree)
                 (.then (fn []
                          (is false "Should have thrown an error")
                          (done)))
                 (.catch (fn [error]
                           (is (some? error) "Should throw error for bad URL")
                           (done))))))))
