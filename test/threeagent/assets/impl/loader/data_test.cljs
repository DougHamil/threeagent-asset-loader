(ns threeagent.assets.impl.loader.data-test
  (:require [cljs.test :refer-macros [deftest is testing async]]
            [threeagent.assets.impl.loader.data :as sut]))

(deftest load-json-keywordized-test
  (testing "JSON file with keywordized keys (default)"
    (async done
           (-> (sut/loader :data/config "/assets/data/config.json" {})
               (.then (fn [data]
                        (is (= "test-config" (:name data)))
                        (is (= 1 (:version data)))
                        (is (= true (get-in data [:settings :debug])))
                        (is (= 3 (get-in data [:settings :maxRetries])))
                        (is (= ["alpha" "beta"] (:tags data)))
                        (done)))
               (.catch (fn [err]
                         (is (nil? err))
                         (done)))))))

(deftest load-json-string-keys-test
  (testing "JSON file with string keys (keywordize-keys false)"
    (async done
           (-> (sut/loader :data/config "/assets/data/config.json" {:keywordize-keys false})
               (.then (fn [data]
                        (is (= "test-config" (get data "name")))
                        (is (= 1 (get data "version")))
                        (is (= true (get-in data ["settings" "debug"])))
                        (done)))
               (.catch (fn [err]
                         (is (nil? err))
                         (done)))))))

(deftest load-edn-test
  (testing "EDN file loads correctly"
    (async done
           (-> (sut/loader :data/levels "/assets/data/levels.edn" {})
               (.then (fn [data]
                        (is (= "Tutorial" (get-in data [:level-1 :name])))
                        (is (= :easy (get-in data [:level-1 :difficulty])))
                        (is (= [10 20 30] (get-in data [:level-1 :enemies])))
                        (is (= "Forest" (get-in data [:level-2 :name])))
                        (done)))
               (.catch (fn [err]
                         (is (nil? err))
                         (done)))))))

(deftest load-error-test
  (testing "missing file produces error"
    (async done
           (-> (sut/loader :data/missing "/assets/data/missing.json" {})
               (.catch (fn [err]
                         (is (some? err))
                         (done)))))))
