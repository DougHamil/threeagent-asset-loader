(ns threeagent.assets.impl.core
  (:require [clojure.walk :refer [postwalk]]
            [clojure.string :as string]))

(defn- join-path [base addition]
  (let [base (if (string/ends-with? base "/")
               (string/join "" (drop-last base))
               base)
        addition (if (string/starts-with? addition "/")
                   (string/join "" (rest addition))
                   addition)]
    (str base "/" addition)))

(defn- leaf? [v]
  (and (vector? v)
       (keyword? (second v))))

(defn- find-refs [e]
  (let [refs (transient [])]
    (->> e
         (postwalk (fn [n]
                     (when (:asset-ref (meta n))
                       (conj! refs (first n)))
                     n)))
    (set (persistent! refs))))

(defmulti visit
  (fn [_ctx node]
    (if (leaf? node)
      :leaf
      :branch)))

(defmethod visit :branch [ctx node]
  (let [path (join-path (:path ctx) (first node))
        [cfg rest] (if (map? (second node))
                     [(second node) (drop 2 node)]
                     [{} (rest node)])
        ;; Depth-first application of middleware
        mw (concat (:middleware cfg [])
                   (:middleware ctx []))
        ctx (merge ctx
                   cfg
                   {:middleware mw
                    :path path})]
    (mapcat (partial visit ctx) rest)))
        
(defmethod visit :leaf [ctx [path key config]]
  (let [leaf-mw (:middleware config [])
        config  (dissoc config :middleware)]
    {key {:key key
          :config config
          :middleware (concat leaf-mw (:middleware ctx []))
          :references (find-refs config)
          :loader (:loader ctx)
          :path (join-path (:path ctx) path)}}))

(defn- detect-duplicates! [nodes]
  (let [dupes (->> nodes
                   (group-by first)
                   (filter #(not= 1 (count (second %))))
                   (map first))]
    (when (seq dupes)
      (throw (ex-info "Duplicate asset keys found" {:duplicate-keys dupes})))))

(defn- detect-cycles!* [nodes visited {:keys [key references]}]
  (if (visited key)
    (throw (ex-info "Cycle detected" {:path visited
                                      :key key}))
    (doseq [n references]
      (detect-cycles!* nodes (conj visited key) (get nodes n)))))

(defn- detect-cycles! [nodes]
  (doseq [[_ n] nodes]
    (detect-cycles!* nodes #{} n)))
  
(defn- validate! [nodes]
  (detect-duplicates! nodes)
  (detect-cycles! (into {} nodes))
  nodes)

(declare get-promise)

(defn- ref->promise [nodes promises database url-resolver on-progress references]
  (js/Promise.all (map
                   #(get-promise nodes promises database url-resolver on-progress (get nodes %))
                   references)))

(defn- resolve-config-refs [database config]
  (->> config
       (postwalk (fn [n]
                   (if (:asset-ref (meta n))
                     (get @database (first n))
                     n)))))

(defn- get-promise [nodes promises database url-resolver on-progress {:keys [key path config references loader middleware]}]
  (if-let [p (get @promises key)]
    p
    (let [resolved-path (url-resolver path)
          p (-> (ref->promise nodes promises database url-resolver on-progress references)
                (.then (fn [_]
                         (let [resolved-config (resolve-config-refs database config)]
                           (-> (loader key resolved-path resolved-config url-resolver path)
                               (.then (fn [data]
                                        [resolved-config data]))))))
                (.then (fn [[resolved-config data]]
                         (let [result (reduce (fn [data mw]
                                                (mw key data resolved-config))
                                              data
                                              middleware)]
                           (swap! database assoc key result)
                           (when on-progress (on-progress))
                           result)))
                (.catch (fn [err]
                          (js/console.error "Failed to load asset %s at path %s due to error:\n%o"
                                            (str key)
                                            path
                                            err)
                          (throw err))))]
      (swap! promises assoc key p)
      p)))

(defn load!
  "Loads the asset tree into the database.

   Optional url-resolver is a function (fn [path] -> resolved-url) that transforms
   asset paths before passing to loaders. Defaults to identity for standard HTTP loading.

   Optional on-progress is a function (fn [loaded total]) called after each asset loads."
  ([database tree]
   (load! database tree identity nil))
  ([database tree url-resolver]
   (load! database tree url-resolver nil))
  ([database tree url-resolver on-progress]
   (let [nodes (->> tree
                    (mapcat (partial visit {:path "./" :middleware []}))
                    (validate!)
                    (into {}))
         total (count nodes)
         loaded (atom 0)
         progress-fn (when on-progress
                       (fn []
                         (let [n (swap! loaded inc)]
                           (on-progress n total))))
         promises (atom {})]
     (js/Promise.all (->> (vals nodes)
                          (map (partial get-promise nodes promises database url-resolver progress-fn)))))))

(defn ref [asset-key]
  ^{:asset-ref true}
  [asset-key])
