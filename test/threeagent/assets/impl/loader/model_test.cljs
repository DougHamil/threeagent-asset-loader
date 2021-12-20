(ns threeagent.assets.impl.loader.model-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            ["three/examples/jsm/loaders/FBXLoader" :refer [FBXLoader]]
            ["three/examples/jsm/loaders/GLTFLoader" :refer [GLTFLoader]]
            [threeagent.assets.impl.loader.model :as sut]))

(deftest loader-selection-test
  (testing "fbx loader is selected for fbx files"
    (is (instance?
         FBXLoader
         (sut/select-loader "/some/asset/path/file.fbx"))))
  (testing "gtlf loader is selected for glb files"
    (is (instance?
         GLTFLoader
         (sut/select-loader "/some/asset/path/file.glb"))))
  (testing "path match is case-insensitive"
    (is (instance?
         FBXLoader
         (sut/select-loader "/some/asset/path/file.FbX")))))
