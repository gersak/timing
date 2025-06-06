;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.gd
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"2nd monday in August" {:name {:en "Carnival Monday"}},
   "02-07" {:name (partial get-name "Independence Day")},
   "10-25" {:name {:en "Thanksgiving Day"}},
   "easter 60" {:name (partial get-name "easter 60")},
   "01-01" {:name (partial get-name "01-01")},
   "10-15 P5D"
   {"type" "observance", :name {:en "Aunty Tek Spice Word Festival"}},
   "tuesday after 2nd monday in August" {:name {:en "Carnival Tuesday"}},
   "easter 50" {:name (partial get-name "easter 50")},
   "04-24 P3D"
   {"type" "observance",
    :name {:en "Carriacou Maroon and String Band Music Festival"}},
   "easter" {"type" "observance", :name (partial get-name "easter")},
   "09-01" {"type" "observance", :name {:en "Kirani Day"}},
   "easter -2" {:name (partial get-name "easter -2")},
   "12-26" {:name (partial get-name "12-26")},
   "easter 49"
   {"type" "observance", :name (partial get-name "easter 49")},
   "easter 1" {:name (partial get-name "easter 1")},
   "12-04 P3D"
   {"type" "observance", :name {:en "Camerhogne Folk Festival"}},
   "05-01" {:name (partial get-name "05-01")},
   "1st monday in August" {:name {:en "Emancipation Day"}},
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

(defmethod is-holiday? :gd
  [_ context]
  (holiday? context))
