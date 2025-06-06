;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.ma
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"10 Dhu al-Hijjah" {:name (partial get-name "10 Dhu al-Hijjah")},
   "01-01" {:name (partial get-name "01-01")},
   "08-14"
   {:name
    {:fr "Journée de Oued Ed-Dahab",
     :ar "يوم وادي الذهب",
     :en "Anniversary of the Recovery Oued Ed-Dahab"}},
   "01-11"
   {:name
    {:fr "Manifeste de l'indépendance",
     :ar "ذكرى تقديم وثيقة الاستقلال",
     :en "Anniversary of the Independence Manifesto"}},
   "1 Muharram" {:name (partial get-name "1 Muharram")},
   "08-21"
   {:name
    {:fr "Journée de la jeunesse", :ar "عيد الشباب", :en "Youth Day"}},
   "1 Shawwal" {:name (partial get-name "1 Shawwal")},
   "08-20"
   {:name
    {:fr "Fête de la révolution du roi et du peuple",
     :ar "ثورة الملك والشعب",
     :en "Anniversary of the Revolution of the King and the People"}},
   "11-06"
   {:name
    {:fr "La Marche verte",
     :ar "المسيرة الخضراء",
     :en "Anniversary of the Green March"}},
   "07-30"
   {:name
    {:fr "Fête du trône", :ar "عيد العرش", :en "Feast of the Throne"}},
   "05-01" {:name (partial get-name "05-01")},
   "11-18"
   {"_name" "Independence Day", :name {:fr "Fête de l'indépendance"}},
   "12 Rabi al-awwal" {:name (partial get-name "12 Rabi al-awwal")}})

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

(defmethod is-holiday? :ma
  [_ context]
  (holiday? context))
