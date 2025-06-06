;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.ru
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"01-01" {:name (partial get-name "01-01")},
   "05-09" {:name {:ru "День Победы", :en "Victory Day"}},
   "11-04" {:name {:ru "День народного единства", :en "Unity Day"}},
   "01-08" {:name {:ru "Новогодние каникулы", :en "New Year Holiday"}},
   "06-12" {:name {:ru "День России", :en "Russia Day"}},
   "03-08" {:name (partial get-name "03-08")},
   "julian 12-25" {:name (partial get-name "12-25")},
   "05-01"
   {:name {:ru "День весны и труда", :en "Spring and Labour Day"}},
   "01-02 P5D"
   {:name {:ru "Новогодние каникулы", :en "New Year Holiday"}},
   "02-23"
   {:name
    {:ru "День защитника Отечества",
     :en "Defender of the Fatherland Day"}}})

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

(defmethod is-holiday? :ru
  [_ context]
  (holiday? context))
