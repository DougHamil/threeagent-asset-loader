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

(def ^:private bad-asset-tree
  [["/assets"
    ["/models" {:loader sut/model-loader}
     ["missing.glb" :model/missing {}]]]])

(deftest failed-load-test
  (async done
         (let [db (atom {})]
           (-> (sut/load! db bad-asset-tree)
               (.catch (fn [error]
                         (is (some? error))
                         (done)))))))

(defn- stub-loader [_key _path cfg]
  (js/Promise.resolve {:loader-saw cfg :tags []}))

(defn- tag-mw [tag]
  (fn [_key data _cfg]
    (update data :tags conj tag)))

(deftest leaf-middleware-test
  (async done
         (let [db (atom {})
               tree [["/assets" {:loader stub-loader
                                 :middleware [(tag-mw :branch)]}
                      ["a.bin" :thing/leaf+branch {:scale 2
                                                   :middleware [(tag-mw :leaf)]}]
                      ["b.bin" :thing/branch-only {:scale 3}]]
                     ["/other" {:loader stub-loader}
                      ["c.bin" :thing/leaf-only {:middleware [(tag-mw :leaf)]}]]]]
           (-> (sut/load! db tree)
               (.then (fn []
                        (let [v1 (:thing/leaf+branch @db)
                              v2 (:thing/branch-only @db)
                              v3 (:thing/leaf-only @db)]
                          (is (= [:leaf :branch] (:tags v1))
                              "leaf middleware runs before branch middleware")
                          (is (= [:branch] (:tags v2))
                              "branch middleware still applies when no leaf middleware")
                          (is (= [:leaf] (:tags v3))
                              "leaf middleware works with no branch middleware")
                          (is (not (contains? (:loader-saw v1) :middleware))
                              ":middleware is stripped from config before loader sees it")
                          (is (= 2 (:scale (:loader-saw v1)))
                              "other config keys still reach the loader")
                          (done))))
               (.catch (fn [err]
                         (is (nil? err))
                         (done)))))))
