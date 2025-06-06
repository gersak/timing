;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.ar
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"2019-07-08" {"type" "bank", :name (partial get-name "Bridge Day")},
   "2019-10-14" {"type" "bank", :name (partial get-name "Bridge Day")},
   "12-08"
   {"active" [{"from" "1995-01-11"}], :name (partial get-name "12-08")},
   "06-10 if tuesday,wednesday then previous monday if thursday,friday then next monday"
   {"active" [{"from" "1988-05-18", "to" "2000-12-15"}],
    :name
    {:en
     "Day of Affirmation of Argentine Rights over the Malvinas, Islands and Antarctic Sector",
     :es
     "Día de la Afirmación de los Derechos Argentinos sobre las Malvinas, Islas y Sector Antártico"}},
   "2023-06-19" {:name (partial get-name "Bridge Day")},
   "06-17 if tuesday,wednesday then previous monday if thursday then next monday"
   {"active" [{"from" "2016-06-11"}],
    :name
    {:en
     "Anniversary of the Passing to Immortality of General Martín Miguel de Güemes",
     :es
     "Día Paso a la Inmortalidad del General Martín Miguel de Güemes"}},
   "2020-03-23" {:name (partial get-name "Bridge Day")},
   "2025-05-02" {"type" "bank", :name (partial get-name "Bridge Day")},
   "easter -48"
   {"active" [{"to" "1976-06-14"} {"from" "2010-11-03"}],
    :name (partial get-name "easter -48")},
   "2013-01-31"
   {:name
    {:en "Bicentennial of the Assembly of the Year XIII",
     :es "Bicentenario de la Asamblea General Constituyente de 1813"}},
   "04-02"
   {"active" [{"from" "2006-07-01"}],
    "disable" ["2020-04-02"],
    "enable" ["2020-03-31"],
    :name
    {:en "Day of the Veterans and the Fallen in the Malvinas War",
     :es "Día del Veterano y de los Caídos en la Guerra de Malvinas"}},
   "2013-02-20"
   {:name
    {:en "Bicentennial of the Battle of Salta",
     :es "Bicentenario de la Batalla de Salta"}},
   "2018-12-31" {"type" "bank", :name (partial get-name "Bridge Day")},
   "2022-10-07" {:name (partial get-name "Bridge Day")},
   "01-01" {:name (partial get-name "01-01")},
   "2023-05-26" {:name (partial get-name "Bridge Day")},
   "2012-04-30" {:name (partial get-name "Bridge Day")},
   "10-12"
   {"active"
    [{"to" "1976-06-14"} {"from" "1982-10-12", "to" "1988-05-23"}],
    :name {:en "The Day of the Race", :es "Día de la Raza"}},
   "easter -47"
   {"active" [{"to" "1976-06-14"} {"from" "2010-11-03"}],
    :name (partial get-name "easter -47")},
   "2021-05-24" {:name (partial get-name "Bridge Day")},
   "12-31 12:00" {"type" "optional", :name (partial get-name "12-31")},
   "2013-06-21" {:name (partial get-name "Bridge Day")},
   "10-12 if tuesday,wednesday then previous monday if thursday,friday,saturday,sunday then next monday"
   {"active" [{"from" "2008-10-03", "to" "2010-11-02"}],
    :name {:en "The Day of the Race", :es "Día de la Raza"}},
   "11-20 if tuesday,wednesday then previous monday if thursday,friday then next monday"
   {"active" [{"from" "2017-01-24"}],
    :name
    {:en "Day of National Sovereignty",
     :es "Día de la Soberanía Nacional"}},
   "3rd monday in August"
   {"active" [{"from" "1995-01-11", "to" "2017-01-23"}],
    "disable" ["2011-08-15"],
    "enable" ["2011-08-22"],
    :name
    {:en
     "Anniversary of the Passing to Immortality of General José de San Martín",
     :es "Paso a la Inmortalidad del General José de San Martín"}},
   "2014-05-02" {:name (partial get-name "Bridge Day")},
   "05-25"
   {:name
    {:en "Day of the First National Government",
     :es "Primer Gobierno Patrio"}},
   "2022-12-09" {:name (partial get-name "Bridge Day")},
   "2015-03-23" {:name (partial get-name "Bridge Day")},
   "2024-06-21" {:name (partial get-name "Bridge Day")},
   "2019-08-19" {"type" "bank", :name (partial get-name "Bridge Day")},
   "04-02 if tuesday,wednesday then previous monday if thursday,friday then next monday"
   {"active" [{"from" "2000-12-22", "to" "2006-06-30"}],
    :name
    {:en "Day of the Veterans and the Fallen in the Malvinas War",
     :es "Día del Veterano y de los Caídos en la guerra en Malvinas"}},
   "2022-05-18"
   {:name {:en "National Census 2022", :es "Censo Nacional 2022"}},
   "2021-11-22" {:name (partial get-name "Bridge Day")},
   "2016-12-09" {:name (partial get-name "Bridge Day")},
   "2020-12-07" {:name (partial get-name "Bridge Day")},
   "2013-04-01" {:name (partial get-name "Bridge Day")},
   "2024-10-11" {:name (partial get-name "Bridge Day")},
   "easter -2" {:name (partial get-name "easter -2")},
   "2012-02-27"
   {:name
    {:en
     "Bicentennial of the Creation and First Oath of Allegiance to the Argentine Flag",
     :es
     "Bicentenario de la creación y primera jura de la bandera argentina"}},
   "4th monday in November"
   {"active" [{"from" "2010-11-03", "to" "2017-01-23"}],
    "disable" ["2015-11-23"],
    "enable" ["2015-11-27"],
    :name
    {:en "Day of National Sovereignty",
     :es "Día de la Soberanía Nacional"}},
   "2016-07-08" {:name (partial get-name "Bridge Day")},
   "06-10"
   {"active" [{"from" "1984-03-23", "to" "1988-05-17"}],
    :name
    {:en
     "Day of Affirmation of Argentine Rights over the Malvinas, Islands and Antarctic Sector",
     :es
     "Día de la Afirmación de los Derechos Argentinos sobre las Malvinas, Islas y Sector Antártico"}},
   "3rd monday in June"
   {"active" [{"from" "1995-01-11", "to" "2010-11-02"}],
    :name {:en "National Flag Day", :es "Día de la Bandera"}},
   "2014-12-26" {:name (partial get-name "Bridge Day")},
   "2015-12-07" {:name (partial get-name "Bridge Day")},
   "1983-04-02"
   {:name
    {:en "Day of the Malvinas, South Georgia and South Sandwich Islands",
     :es
     "Día de las Islas Malvinas, Georgias del Sur y Sandwich del Sur"}},
   "2025-08-15" {"type" "bank", :name (partial get-name "Bridge Day")},
   "06-20"
   {"active"
    [{"from" "1938-06-08", "to" "1988-05-23"}
     {"from" "1991-12-18", "to" "1995-01-10"}
     {"from" "2010-11-03"}],
    :name {:en "National Flag Day", :es "Día de la Bandera"}},
   "2011-03-25" {:name (partial get-name "Bridge Day")},
   "2025-11-21" {"type" "bank", :name (partial get-name "Bridge Day")},
   "05-01" {:name (partial get-name "05-01")},
   "easter -3" {"type" "bank", :name (partial get-name "easter -3")},
   "2021-10-08" {:name (partial get-name "Bridge Day")},
   "2010-10-27"
   {:name {:en "National Census 2010", :es "Censo Nacional 2010"}},
   "03-24"
   {"active" [{"from" "2006-03-21"}],
    :name
    {:en "Day of Remembrance for Truth and Justice",
     :es "Día Nacional de la Memoria por la Verdad y la Justicia"}},
   "2024-04-01" {:name (partial get-name "Bridge Day")},
   "2nd monday in October"
   {"active" [{"from" "2010-11-03", "to" "2017-01-23"}],
    :name
    {:en "Day of Respect for Cultural Diversity",
     :es "Día del Respeto a la Diversidad Cultural"}},
   "08-17"
   {"active"
    [{"from" "1938-09-01", "to" "1988-05-23"}
     {"from" "1994-09-30", "to" "1995-01-10"}],
    :name
    {:en
     "Anniversary of the Passing to Immortality of General José de San Martín",
     :es "Paso a la Inmortalidad del General José de San Martín"}},
   "07-09" {:name (partial get-name "Independence Day")},
   "06-20 if tuesday,wednesday then previous monday if thursday,friday then next monday"
   {"active" [{"from" "1988-05-24", "to" "1991-12-17"}],
    :name {:en "National Flag Day", :es "Día de la Bandera"}},
   "08-17 if tuesday,wednesday then previous monday if thursday,friday then next monday"
   {"active"
    [{"from" "1988-05-24", "to" "1994-09-29"} {"from" "2017-01-24"}],
    :name
    {:en
     "Anniversary of the Passing to Immortality of General José de San Martín",
     :es "Paso a la Inmortalidad del General José de San Martín"}},
   "12-24 12:00" {"type" "optional", :name (partial get-name "12-24")},
   "2012-09-24"
   {:name
    {:en "Bicentennial of the Battle of Tucumán",
     :es "Bicentenario de la Batalla de Tucumán"}},
   "2020-07-10" {:name (partial get-name "Bridge Day")},
   "2012-12-24" {:name (partial get-name "Bridge Day")},
   "2018-12-24" {"type" "bank", :name (partial get-name "Bridge Day")},
   "2018-04-30" {"type" "bank", :name (partial get-name "Bridge Day")},
   "2023-10-13" {:name (partial get-name "Bridge Day")},
   "12-25" {:name (partial get-name "12-25")},
   "2022-11-21" {:name (partial get-name "Bridge Day")},
   "2010-05-24"
   {:name
    {:en "National Holiday for the Bicentennial of the May Revolution",
     :es
     "Feriado Nacional por el Bicentenario de la Revolución de Mayo"}},
   "2011-12-09" {:name (partial get-name "Bridge Day")},
   "10-12 if tuesday,wednesday then previous monday if thursday,friday then next monday #2"
   {"active" [{"from" "2017-01-24"}],
    :name
    {:en "Day of Respect for Cultural Diversity",
     :es "Día del Respeto a la Diversidad Cultural"}},
   "10-12 if tuesday,wednesday then previous monday if thursday,friday then next monday #1"
   {"active" [{"from" "1988-05-24", "to" "2008-10-02"}],
    "disable" ["2001-10-15" "2002-10-12"],
    "enable" ["2001-10-08" "2002-10-14"],
    :name {:en "The Day of the Race", :es "Día de la Raza"}}})

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

(defmethod is-holiday? :ar
  [_ context]
  (holiday? context))
