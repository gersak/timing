;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.to
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"01-01" {:name (partial get-name "01-01")},
   "07-04"
   {:name {:en "Official Birthday of His Majesty King Tupou VI"}},
   "09-17 if thursday,friday,saturday,sunday then next monday and if tuesday then previous monday"
   {:name
    {:en
     "Birthday of His Royal Highness The Crown Prince Tupotoʻa-ʻUlukalala"}},
   "easter -2" {:name (partial get-name "easter -2")},
   "12-04 if thursday,friday,saturday,sunday then next monday and if tuesday then previous monday"
   {:name {:en "Tupou I Day"}},
   "12-26" {:name (partial get-name "12-26")},
   "easter 1" {:name (partial get-name "easter 1")},
   "06-04 if thursday,friday,saturday,sunday then next monday and if tuesday then previous monday"
   {:name {:en "Emancipation Day"}},
   "04-25" {:name {:en "ANZAC Day"}},
   "11-04 if thursday,friday,saturday,sunday then next monday and if tuesday then previous monday"
   {"_name" "Constitution Day", :name {:en "Constitutional Day"}},
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

(defmethod is-holiday? :to
  [_ context]
  (holiday? context))
