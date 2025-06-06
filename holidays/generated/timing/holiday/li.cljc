;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.li
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"12-08" {:name (partial get-name "12-08")},
   "08-15" {"type" "observance", :name {:de "Staatsfeiertag"}},
   "easter 60" {:name (partial get-name "easter 60")},
   "01-01" {:name (partial get-name "01-01")},
   "01-02"
   {"type" "bank", :name {:de "Berchtoldstag", :en "Berchtold Day"}},
   "easter 39" {"_name" "easter 39", :name {:de "Auffahrt"}},
   "easter -47" {"type" "bank", :name (partial get-name "easter -47")},
   "11-01" {"type" "observance", :name (partial get-name "11-01")},
   "easter 50" {:name (partial get-name "easter 50")},
   "easter" {"type" "observance", :name (partial get-name "easter")},
   "12-24" {"type" "bank", :name (partial get-name "12-24")},
   "03-19" {"type" "observance", :name (partial get-name "03-19")},
   "easter -2" {:name (partial get-name "easter -2")},
   "2nd sunday in May"
   {"type" "observance", :name (partial get-name "Mothers Day")},
   "09-08" {:name {:de "Mariä Geburt"}},
   "12-26" {"_name" "12-26", :name {:de "Stephanstag"}},
   "easter 49"
   {"type" "observance", :name (partial get-name "easter 49")},
   "easter 1" {:name (partial get-name "easter 1")},
   "05-01" {:name (partial get-name "05-01")},
   "easter 40" {:name {:de "Feiertagsbrücke"}},
   "01-06" {:name (partial get-name "01-06")},
   "12-31" {"type" "bank", :name (partial get-name "12-31")},
   "02-02" {"type" "observance", :name (partial get-name "02-02")},
   "12-25" {:name (partial get-name "12-25")},
   "easter 61" {:name {:de "Feiertagsbrücke"}}})

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

(defmethod is-holiday? :li
  [_ context]
  (holiday? context))
