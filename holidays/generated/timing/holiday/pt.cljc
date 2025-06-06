;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.pt
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"12-08" {:name (partial get-name "12-08")},
   "08-15" {:name (partial get-name "08-15")},
   "1st sunday in May"
   {"type" "observance", :name (partial get-name "Mothers Day")},
   "easter 60" {:name (partial get-name "easter 60")},
   "01-01" {:name (partial get-name "01-01")},
   "easter -47"
   {"type" "observance", :name (partial get-name "easter -47")},
   "11-01" {:name (partial get-name "11-01")},
   "easter" {:name (partial get-name "easter")},
   "12-24" {"type" "observance", :name (partial get-name "12-24")},
   "10-05" {:name {:pt "Implantação da República"}},
   "easter -2" {:name (partial get-name "easter -2")},
   "12-01" {:name {:pt "Restauração da Independência"}},
   "06-10" {:name {:pt "Dia de Portugal", :en "Portugal Day"}},
   "05-01" {:name (partial get-name "05-01")},
   "04-25" {:name {:pt "Dia da Liberdade", :en "Liberty Day"}},
   "12-31" {"type" "observance", :name (partial get-name "12-31")},
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

(defmethod is-holiday? :pt
  [_ context]
  (holiday? context))
