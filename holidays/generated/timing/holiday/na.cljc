;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.na
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"01-01" {:name (partial get-name "01-01")},
   "easter 39" {:name (partial get-name "easter 39")},
   "05-25" {:name {:en "Africa Day"}},
   "substitutes 05-01 if sunday then next monday"
   {:name "Public Holiday"},
   "substitutes 12-10 if sunday then next monday"
   {:name "Public Holiday"},
   "easter" {"type" "observance", :name (partial get-name "easter")},
   "substitutes 01-01 if sunday then next monday"
   {:name "Public Holiday"},
   "05-04" {:name {:en "Cassinga Day"}},
   "substitutes 08-26 if sunday then next monday"
   {:name "Public Holiday"},
   "easter -2" {:name (partial get-name "easter -2")},
   "12-10" {:name {:en "Human Rights Day"}},
   "substitutes 03-21 if sunday then next monday"
   {:name "Public Holiday"},
   "12-26" {"_name" "12-26", :name {:en "Day of Goodwill"}},
   "easter 1" {:name (partial get-name "easter 1")},
   "05-01" {"_name" "05-01", :name {:en "Workers Day"}},
   "substitutes 05-04 if sunday then next monday"
   {:name "Public Holiday"},
   "03-21" {:name (partial get-name "Independence Day")},
   "08-26" {:name {:en "Heroes' Day"}},
   "substitutes 05-25 if sunday then next monday"
   {:name "Public Holiday"},
   "substitutes 12-26 if sunday then next monday"
   {:name "Public Holiday"},
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

(defmethod is-holiday? :na
  [_ context]
  (holiday? context))
