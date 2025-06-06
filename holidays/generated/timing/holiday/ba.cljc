;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.ba
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"08-15" {:name (partial get-name "08-15")},
   "easter 60" {:name (partial get-name "easter 60")},
   "05-02 and if sunday then next monday"
   {"substitute" true,
    :name {:en "2nd day of the Labour Day", :bs "Drugi dan Dana rada"}},
   "11-01" {:name (partial get-name "11-01")},
   "easter" {:name (partial get-name "easter")},
   "1 Muharram" {:name (partial get-name "1 Muharram")},
   "julian 01-01" {:name (partial get-name "julian 01-01")},
   "1 Shawwal P3D" {:name (partial get-name "1 Shawwal")},
   "julian 08-15" {"note" "orthodox", :name (partial get-name "08-15")},
   "01-01 and if sunday then next tuesday"
   {"substitute" true, :name (partial get-name "01-01")},
   "01-02 and if sunday then next monday"
   {"substitute" true,
    :name {:en "2nd day of the New Year", :bs "Drugi dan Nove Godine"}},
   "12-26" {:name (partial get-name "12-26")},
   "easter 1" {:name (partial get-name "easter 1")},
   "10 Dhu al-Hijjah P4D" {:name (partial get-name "10 Dhu al-Hijjah")},
   "julian 12-25" {:name (partial get-name "julian 12-25")},
   "11-02" {:name (partial get-name "11-02")},
   "05-01 and if sunday then next tuesday"
   {"substitute" true, :name (partial get-name "05-01")},
   "orthodox" {:name (partial get-name "orthodox")},
   "01-06" {:name (partial get-name "01-06")},
   "12 Rabi al-awwal" {:name (partial get-name "12 Rabi al-awwal")},
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

(defmethod is-holiday? :ba
  [_ context]
  (holiday? context))
