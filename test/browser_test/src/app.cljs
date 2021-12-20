(ns app
  (:require [jx.reporter.karma :refer-macros [run-all-tests]]))

(enable-console-print!)

(defn ^:export run-all [karma]
  (run-all-tests karma))
