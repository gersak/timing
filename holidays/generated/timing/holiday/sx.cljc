;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.sx
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"10-09" {:name (partial get-name "Constitution Day")},
   "07-01" {:name {:nl "Emancipatiedag", :en "Emancipation Day"}},
   "04-27" {:name {:nl "Koningsdag", :en "King's Day"}},
   "01-01" {:name (partial get-name "01-01")},
   "easter 39" {:name (partial get-name "easter 39")},
   "easter" {:name (partial get-name "easter")},
   "easter -2" {:name (partial get-name "easter -2")},
   "12-26" {:name (partial get-name "12-26")},
   "easter 49" {:name (partial get-name "easter 49")},
   "easter 1" {:name (partial get-name "easter 1")},
   "05-01 if Sunday then next Monday" {:name (partial get-name "05-01")},
   "04-30 if Saturday then previous Friday if Sunday then next Tuesday since 2017"
   {:name {:nl "Carnaval", :en "Carnival"}},
   "04-30 if Sunday then next Tuesday prior to 2017"
   {:name {:nl "Carnaval", :en "Carnival"}},
   "11-11" {:name {:en "Sint Maarten Day"}},
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

(defmethod is-holiday? :sx
  [_ context]
  (holiday? context))
