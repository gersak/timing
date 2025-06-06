;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.vg
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"Wednesday after 1st Monday in August"
   {:name {:en "Emancipation Wednesday"}},
   "01-01" {:name (partial get-name "01-01")},
   "12-26 and if Saturday then next Monday if Sunday then next Tuesday"
   {"substitute" true, :name (partial get-name "12-26")},
   "Tuesday after 1st Monday in August"
   {:name {:en "Emancipation Tuesday"}},
   "easter 50" {:name (partial get-name "easter 50")},
   "easter" {"type" "observance", :name (partial get-name "easter")},
   "07-01 if Sunday,Thursday then next Monday if Saturday then previous Friday if Tuesday,Wednesday then previous Monday"
   {:name {:en "Virgin Islands Day"}},
   "2nd Friday after 06-02 since 2020"
   {:name {:en "Sovereign’s Birthday"}},
   "2021-11-08" {:name {:en "Commemoration of the Great March of 1949"}},
   "easter -2" {:name (partial get-name "easter -2")},
   "2nd Saturday after 06-02 prior to 2020"
   {"disable" ["2017-06-10" "2019-06-15"],
    "enable" ["2017-06-17" "2019-06-07"],
    :name {:en "Sovereign’s Birthday"}},
   "easter 49"
   {"type" "observance", :name (partial get-name "easter 49")},
   "easter 1" {:name (partial get-name "easter 1")},
   "Monday before 03-08"
   {:name
    {:en "The Anniversary of the Birth of Hamilton Lavity Stoutt"}},
   "2nd Monday in March" {:name {:en "Commonwealth Day"}},
   "1st Monday in August" {:name {:en "Emancipation Monday"}},
   "2021-10-18" {:name {:en "Heroes’ and Forefathers Day"}},
   "12-25 and if Saturday then next Monday if Sunday then next Tuesday"
   {"substitute" true, :name (partial get-name "12-25")},
   "10-21 if Sunday then next Monday if Saturday then previous Friday if Tuesday,Wednesday then previous Monday if Thursday then next Friday"
   {"disable" ["2016-10-23" "2020-10-19" "2021-10-22"],
    "enable" ["2016-10-19" "2020-10-23"],
    :name {:en "St. Ursula’s Day"}}})

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

(defmethod is-holiday? :vg
  [_ context]
  (holiday? context))
