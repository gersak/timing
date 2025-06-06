;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.ge
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"orthodox -1" {:name (partial get-name "easter -1")},
   "03-03" {:name {:ge "დედის დღე", :en "Mother's Day"}},
   "01-01" {:name (partial get-name "01-01")},
   "04-09"
   {:name {:ge "ეროვნული ერთიანობის დღე", :en "National Unity Day"}},
   "01-02" {:name {:ge "ბედობა", :en "Bedoba"}},
   "05-09" {:name {:ge "ფაშიზმზე გამარჯვების დღე", :en "Victory Day"}},
   "10-14" {:name {:ge "სვეტიცხოვლობა", :en "Svetitskhovloba"}},
   "05-26" {:name {:ge "დამოუკიდებლობის დღე", :en "Independence Day"}},
   "08-28" {:name {:ge "მარიამობა", :en "Saint Mary's Day"}},
   "05-12"
   {:name
    {:ge "წმინდა ანდრია პირველწოდებულის ხსენების დღე",
     :en "Saint Andrew the First-Called Day"}},
   "01-19" {:name {:ge "ნათლისღება", :en "Orthodox Epiphany"}},
   "01-07" {:name {:ge "შობა", :en "Orthodox Christmas"}},
   "11-23" {:name {:ge "გიორგობა", :en "Saint George's Day"}},
   "orthodox -2" {:name (partial get-name "easter -2")},
   "03-08" {:name (partial get-name "03-08")},
   "orthodox" {:name (partial get-name "easter")},
   "orthodox 1" {:name (partial get-name "easter 1")}})

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

(defmethod is-holiday? :ge
  [_ context]
  (holiday? context))
