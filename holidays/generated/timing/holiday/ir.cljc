;; This file is autogenerated using timing.holidays.compile/-main

(ns timing.holiday.ir
  (:require
   [clojure.string]
   [timing.holiday :refer [is-holiday?]]
   [timing.holiday.util :refer [parse-definition
                              get-name]]
   [timing.holiday.compiler :as compiler]))

(def holidays
  {"10 Dhu al-Hijjah" {:name (partial get-name "10 Dhu al-Hijjah")},
   "3 Jumada al-thani"
   {:name
    {:fa "ﺷﻬﺎدت ﺣﻀﺮت ﻓﺎﻃﻤﻪ زﻫﺮا ﺳﻼم اﷲ ﻋﻠﯿﻬﺎ",
     :en "Martyrdom of Fatima-Zahara"}},
   "1 Farvardin" {:name {:fa "نوروز", :en "Nowruz"}},
   "28 Safar"
   {:name
    {:fa "رﺣﻠﺖ ﺣﻀﺮت رﺳﻮل اﮐﺮم صلّی الله علیه وآله و سلّم",
     :en "Demise of Prophet Muhammad and Imam Hassan (Mujtaba)"}},
   "20 Safar" {:name {:fa "اربعین حسینی", :en "Arbaeen-e Hosseini"}},
   "17 Rabi al-awwal"
   {:name
    {:fa "ولادت حضرت رسول اکرم صلّی الله علیه وآله و سلّم",
     :en "Birthday of Muhammad Prophet"}},
   "29 Esfand"
   {:name
    {:fa "ملی شدن صنعت نفت",
     :en "Nationalization of the Iranian oil industry"}},
   "13 Rajab"
   {:name
    {:fa "وﻻدت ﺣﻀﺮت اﻣﺎم ﻋﻠﯽ ﻋﻠﯿﻪ اﻟﺴﻼم", :en "Birthday of Imam Ali"}},
   "14 Khordad"
   {:name {:fa "رﺣﻠﺖ اﻣﺎم ﺧﻤﯿﻨﯽ", :en "Imam Khomeini's Demise"}},
   "21 Ramadan"
   {:name {:fa "ﺷﻬﺎدت ﺣﻀﺮت ﻋﻠﯽ ﻋﻠﯿﻪاﻟﺴﻼم", :en "Martyrdom of Imam Ali"}},
   "27 Rajab"
   {:name
    {:fa "ﻣﺒﻌﺚ ﺣﻀﺮت رﺳﻮل اﮐﺮم ﺻﻠﯽ اﷲ ﻋﻠﯿﻪ و آﻟﻪ",
     :en "Mabaas of Muhammad"}},
   "15 Khordad"
   {:name {:fa "قیام ۱۵ خرداد", :en "Revolt of Khordad 15"}},
   "15 Shaban"
   {:name
    {:fa "وﻻدت ﺣﻀﺮت ﻗﺎﺋﻢ ﻋﺠﻞاﷲ ﺗﻌﺎﻟﯽ ﻓﺮﺟﻪ شریف",
     :en "Birthday of Imam Mahdi"}},
   "2 Farvardin" {:name {:fa "نوروز", :en "Nowruz"}},
   "10 Muharram" {:name {:fa "ﻋﺎﺷﻮرای ﺣﺴﯿﻨﯽ", :en "Ashoura"}},
   "1 Shawwal" {:name (partial get-name "1 Shawwal")},
   "3 Farvardin" {:name {:fa "نوروز", :en "Nowruz"}},
   "2 Shawwal"
   {:name {:fa "ﺑﻪ ﻣﻨﺎﺳﺒﺖ ﻋﯿﺪ ﺳﻌﯿﺪ ﻓﻄﺮ", :en "Holyday for Fetr Eeid"}},
   "8 Rabi al-awwal"
   {:name
    {:fa "شهادت امام حسن عسكری علیه‌السلام",
     :en "Martyrdom of Imam Hassan"}},
   "12 Farvardin"
   {:name {:fa "روز جمهوری اسلامی", :en "Islamic Republic Day"}},
   "18 Dhu al-Hijjah"
   {:name {:fa "ﻋﯿﺪ ﺳﻌﯿﺪ ﻏﺪﯾﺮ ﺧﻢ", :en "Eid al-Ghadeer"}},
   "22 Bahman"
   {:name
    {:fa "انقلاب اسلامی پنجاه و هفت",
     :en "Anniversary of Islamic Revolution"}},
   "30 Safar"
   {:name
    {:fa "ﺷﻬﺎدت ﺣﻀﺮت اﻣﺎم رﺿﺎ ﻋﻠﯿﻪاﻟﺴﻼم", :en "Martyrdom of Imam Reza"}},
   "9 Muharram" {:name {:fa "ﺗﺎﺳﻮﻋﺎی ﺣﺴﯿﻨﯽ", :en "Tasoua"}},
   "13 Farvardin" {:name {:fa "سیزده بدر", :en "Sizdah Bedar"}},
   "4 Farvardin" {:name {:fa "نوروز", :en "Nowruz"}},
   "25 Shawwal"
   {:name
    {:fa "ﺷﻬﺎدت ﺣﻀﺮت اﻣﺎم ﺟﻌﻔﺮ ﺻﺎدق ﻋﻠﯿﻪاﻟﺴﻼم",
     :en "Martyrdom of Imam Jafar"}}})

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

(defmethod is-holiday? :ir
  [_ context]
  (holiday? context))
