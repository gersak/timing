;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.ml
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"03-26" {:name {:fr "Journée des Martyrs", :en "Martyrs' Day"}},
   "10 Dhu al-Hijjah" {:name (partial get-name "10 Dhu al-Hijjah")},
   "01-01" {:name (partial get-name "01-01")},
   "01-20" {:name {:fr "Fête de l'armée", :en "Army Day"}},
   "05-25" {:name "Jour de l'Afrique"},
   "1 Shawwal" {:name (partial get-name "1 Shawwal")},
   "easter 1" {:name (partial get-name "easter 1")},
   "05-01" {:name (partial get-name "05-01")},
   "09-22"
   {"_name" "Independence Day",
    :name {:fr "Fête nationale de l'indépendance"}},
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

(defmethod is-holiday? :ml
  [_ context]
  (holiday? context))
