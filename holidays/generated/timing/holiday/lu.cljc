;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.lu
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"08-15" {:name (partial get-name "08-15")},
   "01-01" {:name (partial get-name "01-01")},
   "easter 39" {:name (partial get-name "easter 39")},
   "05-09"
   {"active" [{"from" "2019-01-01"}], :name (partial get-name "05-09")},
   "11-01" {:name (partial get-name "11-01")},
   "easter 50"
   {"active" [{"from" "1892-02-16"}],
    :name (partial get-name "easter 50")},
   "easter" {"type" "observance", :name (partial get-name "easter")},
   "06-23"
   {"active" [{"from" "1961-12-23"}],
    :name (partial get-name "National Holiday")},
   "easter -2"
   {"type" "observance", :name (partial get-name "easter -2")},
   "12-26"
   {"active" [{"from" "1892-02-16"}], :name (partial get-name "12-26")},
   "easter 1"
   {"active" [{"from" "1892-02-16"}],
    :name (partial get-name "easter 1")},
   "05-01"
   {"_name" "05-01",
    "active" [{"from" "1946-04-23"}],
    :name {:fr "1er mai"}},
   "01-23"
   {"active" [{"from" "1947-08-08", "to" "1961-12-23"}],
    :name (partial get-name "National Holiday")},
   "12-25" {:name (partial get-name "12-25")}})

(def locale-holiday-mapping
  (reduce-kv
   (fn [result definition name-mapping]
     (assoc result
            (compiler/compile-type (parse-definition definition))
            name-mapping))
   {}
   holidays))

(defn holiday?
  [context]
  (some
   (fn [[pred naming]]
     (when (pred context)
       naming))
   locale-holiday-mapping))

(defmethod is-holiday? :lu
  [_ context]
  (holiday? context))
