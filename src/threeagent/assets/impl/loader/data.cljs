(ns threeagent.assets.impl.loader.data
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]))

(defn- json-extension? [path]
  (string/ends-with? (string/lower-case path) ".json"))

(defn- parse-content [path content keywordize?]
  (let [data (if (json-extension? path)
               (js->clj (js/JSON.parse content))
               (edn/read-string content))]
    (if keywordize?
      (keywordize-keys data)
      data)))

(defn loader [_key path {:keys [keywordize-keys] :or {keywordize-keys true}}]
  (-> (js/fetch path)
      (.then (fn [response]
               (if (.-ok response)
                 (.text response)
                 (throw (ex-info "Failed to fetch data file"
                                 {:path path
                                  :status (.-status response)})))))
      (.then (fn [content]
               (parse-content path content keywordize-keys)))))
