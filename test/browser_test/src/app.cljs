(ns app
  (:require [jx.reporter.karma :refer-macros [run-all-tests]]
            [cljs.test :refer-macros [deftest is testing]]))

(enable-console-print!)

(defn ^:export run-all [karma]
  (run-all-tests karma))
