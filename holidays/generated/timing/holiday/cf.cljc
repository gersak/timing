;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.cf
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"08-15" {:name (partial get-name "08-15")},
   "10 Dhu al-Hijjah" {:name (partial get-name "10 Dhu al-Hijjah")},
   "01-01" {:name (partial get-name "01-01")},
   "easter 39" {:name (partial get-name "easter 39")},
   "06-30" {:name {:fr "Journée de prière", :en "General Prayer Day"}},
   "11-01" {:name (partial get-name "11-01")},
   "03-29"
   {:name
    {:fr "Décès du Fondateur Barthélémy Boganda", :en "Boganda Day"}},
   "easter 50" {:name (partial get-name "easter 50")},
   "1 Shawwal" {:name (partial get-name "1 Shawwal")},
   "12-01" {:name {:fr "Jour de la République", :en "Republic Day"}},
   "easter 1" {:name (partial get-name "easter 1")},
   "05-01" {:name (partial get-name "05-01")},
   "12-25" {:name (partial get-name "12-25")},
   "08-13" {:name (partial get-name "Independence Day")}})

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

(defmethod is-holiday? :cf
  [_ context]
  (holiday? context))
