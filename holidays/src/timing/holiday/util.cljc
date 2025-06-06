(ns timing.holiday.util
  (:require
   [timing.core :as v]
   [clojure.string :as str])
  #?(:clj
     (:import java.lang.Integer)))

(defn static-holiday
  "Given d as 'day' and m as month, function will generate function that accepts
  timing.core/day-context value and returns true if day and month match input value"
  [d m]
  (fn [{:keys [day-in-month month]}]
    (and (= month m) (= day-in-month d))))

(defn parse-int [x]
  #?(:clj (Integer/parseInt x)
     :cljs (js/parseInt x)))

(defn ->day-time-context
  [year month day]
  (let [dt (-> (v/date year month day)
               v/time->value
               v/day-time-context)]
    dt))

(defn day-name->num
  [day]
  (case day
    "monday" 1
    "tuesday" 2
    "wednesday" 3
    "thursday" 4
    "friday" 5
    "saturday" 6
    "sunday" 7
    nil))

(defn month-name->num
  [month]
  (case month
    "january" 1
    "february" 2
    "march" 3
    "april" 4
    "may" 5
    "june" 6
    "july" 7
    "august" 8
    "september" 9
    "october" 10
    "november" 11
    "december" 12
    nil))

(defn islamic-month-name->num
  "Convert Islamic month name to number (1-12)"
  [month]
  (case (str/lower-case month)
    "muharram" 1
    "safar" 2
    ("rabi" "rabi'" "rabi-al-awwal" "rabi al-awwal") 3
    ("rabi-al-thani" "rabi al-thani") 4
    ("jumada" "jumada-al-awwal" "jumada al-awwal") 5
    ("jumada-al-thani" "jumada al-thani") 6
    "rajab" 7
    ("sha'ban" "shaban") 8
    "ramadan" 9
    "shawwal" 10
    ("dhu-al-qidah" "dhu al-qidah" "dhu-al-qi'dah" "dhu al-qi'dah") 11
    ("dhu-al-hijjah" "dhu al-hijjah") 12
    nil))

; Could extend with Chinese month names

(defn parse-definition
  [text]
  (loop [[word & words] (str/split
                         (str/lower-case
                          (str/replace text #",\s+" ","))
                         #"\s+")
         result nil]
    (if (nil? word) result
        (condp re-find word
          #"orthodox"
          (let [[offset-word & words] words
                offset-number (if (contains? #{"and" nil} offset-word)
                                0
                                (parse-int offset-word))]
            (recur (if (= offset-word "and")
                     (cons offset-word words)
                     words)
                   (assoc result
                     :orthodox? true
                     :offset offset-number)))
          ;;
          #"easter"
          (let [[offset-word & words] words
                offset-number (if (contains? #{"and" nil} offset-word)
                                0
                                (parse-int offset-word))]

            (recur (if (= offset-word "and")
                     (cons offset-word words)
                     words)
                   (assoc result
                     :easter? true
                     :offset offset-number)))
          ;;
          #"julian"
          (recur words
                 (assoc result
                   :julian? true))
          ;;
          #"\d+-\d+-\d+"
          (recur words
                 (let [split-date (str/split word #"(-|#)")
                       year (parse-int (split-date 0))
                       month (parse-int (split-date 1))
                       day (parse-int (split-date 2))]
                   (assoc result
                     :year year
                     :month month
                     :day-in-month day)))
          ;;
          #"\d+-\d+"
          (recur words
                 (let [split-date (str/split word #"(-|#)")
                       day (parse-int (split-date 1))
                       month (parse-int (split-date 0))]
                   (assoc result
                     :day-in-month day
                     :month month)))
          ;;
          #"if$"
          (let [statement (take-while #(not= "if" %) words)]
            (recur
             (drop (count statement) words)
             (update result :statements (fnil conj []) statement)))
          ;;
          #"and$"
          (recur words (assoc result :and? true))
          ;;
          ;; ISO 8601 duration pattern like P2DT, P2DT0H0M - must come before numeric patterns  
          #"p\d+d(?:t(?:\d+h)?(?:\d+m)?)?"
          (let [match (re-find #"p(\d+)d(?:t(?:(\d+)h)?(?:(\d+)m)?)?" word)]
            (recur words
                   (assoc result
                     :period {:days (parse-int (nth match 1))
                              :hours (when (nth match 2) (parse-int (nth match 2)))
                              :minutes (when (nth match 3) (parse-int (nth match 3)))})))
          ;;
          #"\d[a-z]{2}"
          (recur words (assoc result :nth (parse-int ((str/split word #"") 0))))
          ;;
          #"(monday|tuesday|wednesday|thursday|friday|saturday|sunday)"
          (recur words (assoc result :week-day (day-name->num word)))
          ;;
          #"^in$"
          (let [[target-month & words] words
                target-month-number (month-name->num target-month)
                results (if (nil? target-month-number) ;; ????
                          (assoc result
                            :unknown [target-month])
                          (assoc result
                            :in? true
                            :month target-month-number))]
            (recur words results))
          ;;
          #"(before|after)"
          (let [predicate (if (= word "before") :before :after)
                temp (take-while #(nil? (re-find #"(\d+-\d+|january|february|march|april|may|june|july|august|september|october|november|december)" %)) words)
                n (+ (count temp) 1)
                what (take n words)]
            (recur (drop (count what) words)
                   (assoc result
                     :predicate predicate
                     :relative-to (parse-definition (str/join " " what)))))
          ;;
          #"(january|february|march|april|may|june|july|august|september|october|november|december)"
          (recur words (assoc result :month (month-name->num word)))
          ;;
          ;; Islamic calendar months - handle single and multi-word names
          #"(muharram|safar|rabi|jumada|rajab|sha'ban|shaban|ramadan|shawwal|dhu)"
          (let [;; Try single word first
                single-month (islamic-month-name->num word)
                ;; Try combining with next word for multi-word names like "dhu al-hijjah"
                [next-word & remaining-words] words
                combined-word (when next-word (str word " " next-word))
                combined-month (when combined-word (islamic-month-name->num combined-word))]
            (cond
              ;; Found two-word Islamic month and there's a pending day  
              (and combined-month (:temp-day result))
              (recur remaining-words (-> result
                                         (dissoc :temp-day)
                                         (assoc :islamic? true
                                                :day-in-month (:temp-day result)
                                                :month combined-month)))

              ;; Found single-word Islamic month and there's a pending day
              (and single-month (:temp-day result))
              (recur words (-> result
                               (dissoc :temp-day)
                               (assoc :islamic? true
                                      :day-in-month (:temp-day result)
                                      :month single-month)))

              ;; Islamic month word but no pending day - treat as unknown
              :else
              (recur words (update result :unknown (fnil conj []) word))))
          ;;
          ;; Check if word is a number (potential Islamic day)
          #"^\d+$"
          (let [day-num (parse-int word)]
            (if (and (>= day-num 1) (<= day-num 30))
              ;; Store the day number temporarily in case next word is Islamic month
              (recur words (assoc result :temp-day day-num))
              ;; Not a valid day number
              (recur words (update result :unknown (fnil conj []) word))))
          ;;
          ;; ISO 8601 duration pattern like P2DT, P2DT0H0M
          #"p\d+d(?:t(?:\d+h)?(?:\d+m)?)?"
          (let [match (re-find #"p(\d+)d(?:t(?:(\d+)h)?(?:(\d+)m)?)?" word)]
            (recur words
                   (assoc result
                     :period {:days (parse-int (nth match 1))
                              :hours (when (nth match 2) (parse-int (nth match 2)))
                              :minutes (when (nth match 3) (parse-int (nth match 3)))})))
          ;;
          (recur
           words
           (update result :unknown (fnil conj []) word))))))

 ;; Test period parsing separately
(comment
  (parse-definition "01-01 P2DT")
  (parse-definition "P2DT"))

(def names
  {"12-08"
   {"EN" "Immaculate Conception",
    "DE" "Mariä Empfängnis",
    "ES" "La inmaculada concepción",
    "FR" "Immaculée Conception",
    "IT" "Immacolata Concezione",
    "MT" "Il-Kunċizzjoni",
    "NL" "Onbevlekte Ontvangenis van Maria",
    "PT" "Imaculada Conceição"},
   "easter -46"
   {"EN" "Ash Wednesday",
    "IT" "Ceneri",
    "IS" "Öskudagur",
    "PT" "Quarta-feira de Cinzas",
    "HU" "Hamvazószerda",
    "SW" "Jumatano ya Majivu",
    "FR" "Mercredi des Cendres",
    "DE" "Aschermittwoch",
    "NL" "Aswoensdag",
    "VI" "Thứ tư Lễ Tro",
    "ES" "Miercoles de Ceniza"},
   "08-15"
   {"EN" "Assumption",
    "IT" "Ferragosto",
    "MK" "Успение на Пресвета Богородица",
    "LT" "Žolinė",
    "PT" "Assunção de Maria",
    "HR" "Velika Gospa",
    "EL" "Κοίμηση της Θεοτόκου",
    "RO" "Adormirea Maicii Domnului",
    "SQ" "Shën Mëria e Gushtit",
    "FR" "Assomption",
    "MG" "Asompsiona",
    "DE" "Mariä Himmelfahrt",
    "NL" "O.L.V. Hemelvaart",
    "VI" "Đức Mẹ Lên Trời",
    "ES" "Asunción",
    "SL" "Marijino vnebovzetje",
    "MT" "Santa Marija",
    "PL" "Wniebowzięcie Najświętszej Maryi Panny"},
   "10 Dhu al-Hijjah"
   {"EN" "Feast of the Sacrifice (Eid al-Adha)",
    "FA" "ﻋﯿﺪ ﺳﻌﯿﺪ ﻗﺮﺑﺎن",
    "AM" "ዒድ አል አድሐ",
    "MK" "Курбан Бајрам",
    "BN" "ঈদুল আযহা",
    "HR" "Kurban-bajram",
    "SR" "Курбански Бајрам",
    "FIL" "Eidul Adha",
    "SW" "Idd-ul-Azha",
    "AR" "عيد الأضحى",
    "ID" "Hari Raya Idul Adha",
    "TR" "Kurban Bayramı",
    "SQ" "Kurban Bajrami",
    "FR" "Fête du mouton",
    "MS" "Hari Raya Haji",
    "DE" "Opferfest",
    "NL" "Offerfeest (Eid Al-Adha)",
    "BS" "Kurbanski bajram",
    "AZ" "Qurban Bayramı"},
   "easter -48"
   {"EN" "Shrove Monday",
    "DE" "Rosenmontag",
    "ES" "Carnaval",
    "FR" "Lundi de Carnaval",
    "NL" "Carnavalmaandag",
    "PAP" "Dialuna di Carnaval",
    "VI" "Ngày thứ hai hoa hồng"},
   "Independence Day"
   {"UK" "День Незалежності",
    "EN" "Independence Day",
    "FI" "Itsenäisyyspäivä",
    "MK" "Ден на независноста",
    "BG" "Ден на независимостта",
    "PT" "Dia da Independência",
    "HR" "Dan neovisnosti",
    "SR" "Дан независности",
    "FIL" "Araw ng Kalayaan",
    "SW" "Siku ya uhuru",
    "RO" "Ziua Independentei",
    "AR" "عيد الاستقلال",
    "TI" "መዓልቲ ናጽነት",
    "ID" "Hari Ulang Tahun Kemerdekaan Republik Indonesia",
    "SV" "Självständighetsdagen",
    "HY" "Անկախության օր",
    "SQ" "Dita e Pavarësisë",
    "FR" "Jour de l'Indépendance",
    "MS" "Hari Kebangsaan",
    "DE" "Unabhängigkeitstag",
    "ET" "iseseisvuspäev",
    "BE" "Дзень Незалежнасцi",
    "NL" "Onafhankelijkheidsdag",
    "VI" "Ngày Độc lập",
    "BS" "Dan nezavisnosti",
    "ES" "Día de la Independencia",
    "MT" "Jum l-Indipendenza",
    "PL" "Narodowe Święto Niepodległości"},
   "easter 60"
   {"EN" "Corpus Christi",
    "IT" "Corpus Domini",
    "PT" "Corpo de Deus",
    "HR" "Tijelovo",
    "FR" "la Fête-Dieu",
    "DE" "Fronleichnam",
    "NL" "Sacramentsdag",
    "VI" "Lễ Mình và Máu Thánh Chúa Kitô",
    "ES" "Corpus Christi",
    "PL" "Dzień Bożego Ciała"},
   "Public Holiday"
   {"EN" "Public Holiday",
    "FR" "Jour férié légaux",
    "NL" "Wettelijke feestdag",
    "PT" "Feriado Obrigatório",
    "VI" "Nghỉ lễ Toàn Quốc"},
   "01-01"
   {"UK" "Новий Рік",
    "EN" "New Year's Day",
    "RU" "Новый год",
    "IT" "Capodanno",
    "FI" "Uudenvuodenpäivä",
    "AM" "እንቁጣጣሽ",
    "KO" "신정",
    "MK" "Нова Година",
    "PAP" "Aña Nobo",
    "IS" "Nýársdagur",
    "LT" "Naujieji metai",
    "BG" "Нова Година",
    "PT" "Ano Novo",
    "HR" "Nova godina",
    "GE" "ახალი წელი",
    "EL" "Πρωτοχρονιά",
    "HU" "Újév",
    "SR" "Нова година",
    "FIL" "Araw ng Bagong Taon",
    "SW" "Mwaka mpya",
    "JP" "元日",
    "RO" "Anul nou",
    "AR" "عيد رأس السنة",
    "TI" "ሓዲሽ ዓመት",
    "ID" "Hari tahun baru",
    "SV" "Nyårsdagen",
    "TR" "Yılbaşı",
    "HY" "Ամանոր",
    "SQ" "Viti i Ri",
    "KL" "ukiortaaq",
    "FR" "Nouvel An",
    "MS" "Hari Tahun Baru",
    "CA" "Any nou",
    "MG" "Taom-baovao",
    "DE" "Neujahr",
    "NO" "Første nyttårsdag",
    "ET" "uusaasta",
    "DA" "Nytår",
    "BE" "Новы год",
    "CZ" "Nový rok",
    "NL" "Nieuwjaar",
    "FO" "Nýggjársdagur",
    "VI" "Tết Dương lịch",
    "LV" "Jaunais Gads",
    "BS" "Novogodisnji dan",
    "AZ" "Yeni il",
    "ES" "Año Nuevo",
    "ZH" "元旦",
    "SL" "Novo leto",
    "MT" "L-Ewwel tas-Sena",
    "PL" "Nowy Rok"},
   "27 Ramadan"
   {"EN" "Laylat al-Qadr",
    "AR" "لیلة القدر",
    "BS" "Lejletul kadr",
    "NL" "Waardevolle Nacht (Laylat al-Qadr)",
    "SQ" "Nata e Kadrit"},
   "easter 39"
   {"EN" "Ascension Day",
    "IT" "Ascensione",
    "FI" "Helatorstai",
    "PAP" "Dia di Asuncion",
    "IS" "Uppstigningardagur",
    "RO" "Ziua Eroilor",
    "ID" "Kenaikan Yesus Kristus",
    "SV" "Kristi himmelfärds dag",
    "KL" "qilaliarfik",
    "FR" "Ascension",
    "MG" "Andro niakarana",
    "DE" "Christi Himmelfahrt",
    "NO" "Kristi himmelfartsdag",
    "DA" "Kristi Himmelfartsdag",
    "NL" "O.L.H. Hemelvaart",
    "FO" "Kristi Himmalsferðardagur",
    "VI" "Lễ Thăng Thiên",
    "ES" "La Asunción"},
   "easter -47"
   {"EN" "Shrove Tuesday",
    "ES" "Carnaval",
    "DE" "Faschingsdienstag",
    "HR" "Pokladni utorak",
    "HU" "Húshagyó kedd",
    "NL" "Vastenavond",
    "PT" "Carnaval",
    "VI" "Thứ ba mập béo"},
   "05-09"
   {"EN" "Europe Day",
    "IT" "Festa dell'Europa",
    "FI" "Eurooppa-päivä",
    "CS" "Den Evropy",
    "GA" "Lá na hEorpa",
    "LS" "Dan Evrope",
    "LT" "Europos diena",
    "BG" "Денят на Европа",
    "PT" "Dia da Europa",
    "HR" "Dan Europe",
    "EL" "Ημέρα της Ευρώπης",
    "HU" "Európa-nap",
    "RO" "Ziua Europei",
    "SV" "Europadagen",
    "SQ" "Dita e Evropës",
    "FR" "Journée de l'Europe",
    "DE" "Europatag",
    "ET" "Euroopa päev",
    "DA" "Europadagen",
    "NL" "Dag van Europa ou Europadag",
    "LV" "Eiropas diena",
    "SK" "Deň Európy",
    "ES" "Día de Europa",
    "MT" "Jum l-Ewropa",
    "PL" "Dzień Europy"},
   "06-29"
   {"EN" "Saints Peter and Paul",
    "DE" "Peter und Paul",
    "ES" "San Pedro y San Pablo",
    "FR" "Saint Pierre et Paul",
    "IT" "Santi Pietro e Paolo",
    "MT" "L-Imnarja",
    "NL" "Hoogfeest van Petrus en Paulus",
    "VI" "Lễ kính Thánh Phêrô"},
   "17 Ramadan"
   {"EN" "Day of Nuzul Al-Quran", "MS" "Hari Nuzul Al-Quran"},
   "27 Rajab"
   {"EN" "Laylat al-Mi'raj",
    "AR" "الإسراء والمعراج",
    "BS" "Lejletul Mi'radž",
    "ID" "Maulid Nabi Muhammad",
    "MS" "Israk dan Mikraj",
    "NL" "Laylat al-Miraadj",
    "SQ" "Nata e Miraxhit",
    "TR" "Miraç Gecesi"},
   "11-01"
   {"EN" "All Saints' Day",
    "IT" "Ognissanti",
    "FI" "Pyhäinpäivä",
    "MK" "Празникот на сите светци",
    "LT" "Visų šventųjų diena",
    "PT" "Todos os santos",
    "HR" "Svi sveti",
    "HU" "Mindenszentek",
    "SR" "Сви Свети",
    "FIL" "Undás; Todos los Santos; Araw ng mga Santo",
    "SV" "Alla Helgons dag",
    "SQ" "Të gjitha Saints",
    "FR" "Toussaint",
    "MG" "Fetin'ny olo-masina",
    "DE" "Allerheiligen",
    "NL" "Allerheiligen",
    "VI" "Lễ Các Thánh",
    "BS" "Dita e të gjithë Shenjtorëve",
    "SK" "Sviatok všetkých svätých",
    "ES" "Todos los Santos",
    "PL" "Wszystkich Świętych"},
   "easter -1"
   {"EN" "Easter Saturday",
    "IT" "Sabado santo",
    "GE" "დიდი შაბათი",
    "HU" "Nagyszombat",
    "FIL" "Sabado de Gloria",
    "SV" "Påskafton",
    "FR" "Samedi saint",
    "DE" "Karsamstag",
    "NO" "Påskeaften",
    "CZ" "Bílá sobota",
    "NL" "Dag voor Pasen",
    "VI" "Thứ bảy Tuần Thánh",
    "ES" "Sabado Santo",
    "ZH" "耶穌受難節翌日"},
   "Mothers Day"
   {"EN" "Mother's Day",
    "IT" "Festa della mamma",
    "FI" "Äitienpäivä",
    "IS" "Mæðradagurinn",
    "LT" "Motinos diena",
    "PT" "Dia das Mães",
    "HR" "Majčin dan",
    "EL" "Γιορτή της μητέρας",
    "HU" "Anyák napja",
    "RO" "Ziua Mamei",
    "SV" "Mors dag",
    "HY" "Ցեղասպանության զոհերի հիշատակի օր",
    "SQ" "Dita e Nënës",
    "FR" "Fête des Mères",
    "DE" "Muttertag",
    "NO" "Morsdag",
    "ET" "emadepäev",
    "DA" "Mors Dag",
    "CZ" "Den matek",
    "NL" "Moederdag",
    "VI" "Ngày của mẹ",
    "LV" "Mātes diena",
    "ES" "Día de la Madre",
    "PL" "Dzień Matki"},
   "easter 50"
   {"EN" "Whit Monday",
    "IT" "Lunedì di Pentecoste",
    "IS" "Annar í hvítasunnu",
    "EL" "Αγίου Πνεύματος",
    "HU" "Pünkösdhétfő",
    "RO" "Două zi de Rusalii",
    "SV" "Annandag pingst",
    "KL" "piinsip aappaa",
    "FR" "Lundi de Pentecôte",
    "MG" "Alatsinain'ny Pentekosta",
    "DE" "Pfingstmontag",
    "NO" "Andre pinsedag",
    "DA" "Anden Pinsedag",
    "NL" "Tweede pinksterdag",
    "FO" "Annar hvítusunnudagur",
    "ES" "Lunes de Pentecostés"},
   "Reformation Day"
   {"EN" "Reformation Day",
    "DE" "Reformationstag",
    "ES" "Día Nacional de las Iglesias Evangélicas y Protestantes",
    "NL" "Hervormingsdag",
    "VI" "Kháng Cách"},
   "Vesak" {"EN" "Vesak Day", "ID" "Hari Raya Waisak"},
   "15 Shaban"
   {"EN" "Laylat al-Bara'at",
    "AR" "ليلة البراءة",
    "BS" "Lejletul berat",
    "NL" "Laylat al-Baraat",
    "SQ" "Nata e Beratit"},
   "02-14"
   {"EN" "Valentine's Day",
    "DE" "Valentinstag",
    "FR" "Saint-Valentin",
    "HU" "Valentin nap",
    "NL" "Valentijnsdag",
    "VI" "Lễ tình nhân"},
   "Bridge Day" {"EN" "Bridge Day", "ES" "Feriado Puente Turístico"},
   "easter"
   {"EN" "Easter Sunday",
    "IT" "Domenica di Pasqua",
    "FI" "Pääsiäispäivä",
    "AM" "ፋሲካ",
    "PAP" "Dia Pasco di Resureccion",
    "IS" "Páskadagur",
    "LT" "Velykos",
    "BG" "Великден",
    "PT" "Páscoa",
    "HR" "Uskrs",
    "GE" "აღდგომა",
    "EL" "Πάσχα",
    "HU" "Húsvétvasárnap",
    "SR" "Католички Васкрс",
    "FIL" "Pasko ng Pagkabuhay",
    "SW" "Pasaka",
    "RO" "Paștele",
    "SV" "Påskdagen",
    "SQ" "Pashkët Katolike",
    "KL" "poorskip-ullua",
    "FR" "Pâques",
    "DE" "Ostersonntag",
    "NO" "Første påskedag",
    "ET" "lihavõtted",
    "DA" "Påskesøndag",
    "CZ" "Velikonoční neděle",
    "NL" "Pasen",
    "FO" "Páskadagur",
    "VI" "Lễ Phục Sinh",
    "LV" "Lieldienas",
    "BS" "Vaskrs",
    "SK" "Veľká noc",
    "ES" "Pascua",
    "ZH" "复活节",
    "SL" "Velika noč",
    "PL" "Niedziela Wielkanocna"},
   "Constitution Day"
   {"UK" "День Конституції",
    "EN" "Constitution Day",
    "KO" "제헌절",
    "PT" "Dia da Constituição",
    "FIL" "Araw ng Saligang Batas",
    "JP" "憲法記念日",
    "RO" "Ziua Constituției",
    "HY" "Սահմանադրության օր",
    "SQ" "Dita e Kushtetutës",
    "CA" "Dia de la Constitució",
    "DE" "Tag der Verfassung",
    "NO" "Grunnlovsdagen",
    "DA" "Grundlovsdag",
    "NL" "Dag van de Grondwet",
    "FO" "Grundlógardagur",
    "VI" "Ngày pháp luật",
    "SK" "Deň Ústavy",
    "ES" "Día de la Constitución"},
   "15 Nisan"
   {"EN" "Pesach",
    "BS" "Pesah",
    "DE" "Pessach",
    "HR" "Pesač",
    "NL" "Pesach",
    "SQ" "Pesach",
    "SR" "Песах"},
   "12-24"
   {"EN" "Christmas Eve",
    "FI" "Jouluaatto",
    "IS" "Aðfangadagur",
    "LT" "Šv. Kūčios",
    "BG" "Бъдни вечер",
    "PT" "Noite de Natal",
    "HR" "Badnji dan",
    "HU" "Szenteste",
    "SR" "Бадњи дан",
    "FIL" "Bisperas ng Pasko",
    "SV" "Julafton",
    "SQ" "Nata e Krishtlindjes",
    "KL" "juulliaraq",
    "FR" "Veille de Noël",
    "MS" "Hari Sebelum Krismas",
    "DE" "Heiliger Abend",
    "NO" "Julaften",
    "ET" "jõululaupäev",
    "DA" "Juleaften",
    "CZ" "Štědrý den",
    "NL" "Kerstavond",
    "FO" "Jólaaftan",
    "VI" "Đêm Giáng Sinh",
    "LV" "Ziemassvētku vakars",
    "BS" "Badnji dan",
    "SK" "Štedrý deň",
    "ES" "Nochebuena"},
   "julian 12-24"
   {"EN" "Orthodox Christmas Eve",
    "NL" "Orthodox Kerstavond",
    "MK" "Бадник",
    "TI" "ልደት"},
   "1 Muharram"
   {"EN" "Islamic New Year",
    "HR" "Nova hidžretska godina",
    "FIL" "Unang Araw ng Muharram",
    "AR" "رأس السنة الهجرية",
    "ID" "Tahun Baru Islam",
    "SQ" "Viti i Ri hixhri",
    "FR" "Nouvel an islamique",
    "MS" "Awal Muharram",
    "NL" "Islamitisch Nieuwjaar",
    "BS" "Nova hidžretska godina"},
   "julian 01-01"
   {"EN" "Orthodox New Year",
    "BS" "Pravoslavni novogodišnji dan",
    "HR" "Pravoslavna Nova Godina",
    "NL" "Orthodox Nieuwjaar",
    "SQ" "Viti i Ri Ortodoks",
    "SR" "Православна Нова година"},
   "Revolution Day"
   {"EN" "Revolution Day",
    "AR" "يوم الثورة",
    "ES" "Día de la Revolución",
    "NL" "Dag van de revolutie",
    "TI" "ባሕቲ መስከረም",
    "VI" "Tổng khởi nghĩa"},
   "1 Tishrei"
   {"EN" "Rosh Hashanah",
    "BS" "Roš Hašana",
    "DE" "Rosch Haschana",
    "HR" "Roš Hašane",
    "NL" "Rosj Hasjana",
    "SQ" "Rosh Hashanah",
    "SR" "Рош Хашана"},
   "03-19"
   {"EN" "Saint Joseph",
    "DE-AT" "Josefitag",
    "DE" "Josefstag",
    "ES" "San José",
    "IT" "San Giuseppe",
    "MT" "San Ġużepp",
    "NL" "Hoogfeest van de Heilige Jozef",
    "VI" "Kính Thánh Giuse"},
   "easter -2"
   {"EN" "Good Friday",
    "IT" "Venerdì santo",
    "FI" "Pitkäperjantai",
    "AM" "ስቅለት",
    "PAP" "Diabierna Santo",
    "IS" "Föstudagurinn langi",
    "BG" "Разпети петък",
    "PT" "Sexta-Feira Santa",
    "HR" "Veliki petak",
    "GE" "წითელი პარასკევი",
    "EL" "Μεγάλη Παρασκευή",
    "HU" "Nagypéntek",
    "SR" "Католички Велики петак",
    "FIL" "Biyernes Santo",
    "SW" "Ijumaa Kuu",
    "RO" "Vinerea Mare",
    "ID" "Wafat Yesus Kristus",
    "SV" "Långfredagen",
    "SQ" "E Premtja e Madhe",
    "KL" "tallimanngornersuaq",
    "FR" "Vendredi saint",
    "MS" "Jumat Agung",
    "DE" "Karfreitag",
    "NO" "Langfredag",
    "ET" "suur reede",
    "DA" "Langfredag",
    "CZ" "Velký pátek",
    "NL" "Goede Vrijdag",
    "FO" "Langafríggjadagur",
    "VI" "Thứ sáu Tuần Thánh",
    "LV" "Lielā Piektdiena",
    "BS" "Dobar petak",
    "SK" "Veľkonočný piatok",
    "ES" "Viernes Santo",
    "ZH" "耶穌受難節",
    "MT" "Il-Ġimgħa l-Kbira"},
   "10 Muharram"
   {"EN" "Day of Ashura", "AR" "عاشوراء", "BN" "আশুরা", "NL" "Asjoera"},
   "1 Shawwal"
   {"EN" "End of Ramadan (Eid al-Fitr)",
    "FA" "ﻋﯿﺪ ﺳﻌﯿﺪ ﻓﻄﺮ",
    "AM" "ዒድ አል ፈጥር",
    "MK" "Рамазан Бајрам",
    "BN" "ঈদুল ফিতর",
    "HR" "Ramazanski bajram",
    "SR" "Рамазански Бајрам",
    "FIL" "Pagwawakas ng Ramadan",
    "SW" "Idd-ul-Fitr",
    "AR" "عيد الفطر",
    "ID" "Hari Raya Idul Fitri",
    "TR" "Ramazan Bayramı",
    "SQ" "Fitër Bajrami",
    "FR" "Fête de fin du Ramadan",
    "MS" "Hari Raya Aidil Fitri",
    "DE" "Zuckerfest",
    "NL" "Suikerfeest (Eid al-Fitr)",
    "BS" "Ramazanski bajram",
    "AZ" "Ramazan Bayramı"},
   "23 Ramadan" {"EN" "Lailat al-Qadr"},
   "12-06"
   {"EN" "Saint Nicholas",
    "DE" "Sankt Nikolaus",
    "FR" "Saint-Nicolas",
    "HU" "Mikulás",
    "NL" "Sinterklaas",
    "VI" "Thánh Saint Nicholas"},
   "Buß- und Bettag"
   {"DE" "Buß- und Bettag", "EN" "Day of Prayer and Repentance"},
   "12-26"
   {"DE-AT" "Stefanitag",
    "EN" "Boxing Day",
    "IT" "Santo Stefano",
    "FI" "2. joulupäivä",
    "DE-CH" "Stephanstag",
    "PAP" "Di dos Dia Pasco di Nascimento",
    "IS" "Annar í jólum",
    "LT" "2. Kalėdų diena",
    "BG" "2-ри ден на Коледа",
    "HR" "Svetog Stjepana",
    "EL" "Δεύτερη μέρα των Χριστουγέννων",
    "HU" "Karácsony másnapja",
    "RO" "Două zi de Crăciun",
    "SV" "Annandag jul",
    "KL" "juullip aappaa",
    "FR" "Lendemain de Noël",
    "DE" "2. Weihnachtstag",
    "NO" "Andre juledag",
    "ET" "teine jõulupüha",
    "DA" "Anden Juledag",
    "CZ" "2. svátek vánoční",
    "NL" "Tweede kerstdag",
    "FO" "Fyrsti gerandisdagur eftir jóladag",
    "VI" "Ngày tặng quà",
    "LV" "Otrie Ziemassvētki",
    "SK" "Druhý sviatok vianočný",
    "ES" "San Esteban",
    "PL" "Drugi dzień Bożego Narodzenia"},
   "9 Dhu al-Hijjah"
   {"EN" "Arafat Day", "AR" "يوم عرفة", "MS" "Hari Arafah"},
   "easter 49"
   {"UK" "Трійця",
    "EN" "Pentecost",
    "IT" "Pentecoste",
    "FI" "Helluntaipäivä",
    "MK" "Духовден",
    "IS" "Hvítasunnudagur",
    "EL" "Πεντηκοστή",
    "HU" "Pünkösdvasárnap",
    "RO" "Rusaliile",
    "SV" "Pingstdagen",
    "KL" "piinsip ullua",
    "FR" "Pentecôte",
    "DE" "Pfingstsonntag",
    "NO" "Første pinsedag",
    "ET" "nelipühade 1. püha",
    "DA" "Pinsedag",
    "NL" "Pinksteren",
    "FO" "Hvítusunnudagur",
    "VI" "Lễ Chúa Thánh Thần Hiện Xuống",
    "ES" "Pentecostés",
    "SL" "Binkošti",
    "PL" "Zielone Świątki"},
   "easter 1"
   {"EN" "Easter Monday",
    "IT" "Lunedì dell’Angelo",
    "FI" "2. pääsiäispäivä",
    "MK" "вториот ден на Велигден",
    "PAP" "Di dos Dia Pasco di Resureccion",
    "IS" "Annar í páskum",
    "BG" "Велики понеделник",
    "HR" "Uskršnji ponedjeljak",
    "GE" "აღდგომის ორშაბათი",
    "EL" "Δευτέρα του Πάσχα",
    "HU" "Húsvéthétfő",
    "SR" "Католички Васкрсни понедељак",
    "SW" "Jumatatu ya Pasaka",
    "RO" "Două zi de Pasti",
    "SV" "Annandag påsk",
    "KL" "poorskip-aappaa",
    "FR" "Lundi de Pâques",
    "MG" "Alatsinain'ny Paska",
    "DE" "Ostermontag",
    "NO" "Andre påskedag",
    "DA" "Anden påskedag",
    "CZ" "Velikonoční pondělí",
    "NL" "Tweede paasdag",
    "FO" "Annar páskadagur",
    "VI" "Thứ hai phục sinh",
    "LV" "Otrās Lieldienas",
    "BS" "Uskrsni ponedjeljak",
    "SK" "Veľkonočný pondelok",
    "ES" "Lunes de Pascua",
    "ZH" "復活節星期一",
    "SL" "Velikonočni ponedeljek",
    "PL" "Drugi dzień Wielkanocy"},
   "Liberation Day"
   {"EN" "Liberation Day",
    "AR" "يوم التحرير",
    "NL" "Bevrijdingsdag",
    "SQ" "Dita e Çlirimit",
    "VI" "Ngày Thống nhất",
    "NO" "Frigjøringsdagen"},
   "orthodox -2"
   {"EN" "Orthodox Good Friday",
    "MK" "Велики Петок",
    "NL" "Orthodoxe Goede vrijdag",
    "SR" "Велики петак",
    "TI" "ዓርቢ ስቅለት"},
   "03-08"
   {"UK" "Міжнародний жіночий день",
    "EN" "International Women's Day",
    "RU" "Международный женский день",
    "BG" "Ден на жената",
    "PT" "Dia Internacional da Mulher",
    "GE" "ქალთა საერთაშორისო დღე",
    "HU" "Nemzetközi nőnap",
    "RO" "Ziua Internationala a Femeii",
    "TI" "መዓልቲ ኣነስቲ",
    "HY" "Կանանց տոն",
    "FR" "Journée internationale des femmes",
    "DE" "Internationaler Frauentag",
    "BE" "Мiжнародны жаночы дзень",
    "NL" "Internationale Vrouwendag",
    "VI" "Quốc tế Phụ nữ",
    "AZ" "Qadınlar günü",
    "ZH" "国际妇女节",
    "SL" "Mednarodni dan žena"},
   "julian 12-25"
   {"UK" "Різдво",
    "EN" "Orthodox Christmas",
    "MK" "Прв ден Божик",
    "HR" "Pravoslavni Božić",
    "SR" "Божић",
    "RO" "Craciun pe Rit Vechi",
    "SQ" "Krishtlindjet Ortodokse",
    "NL" "Orthodox Kerstmis",
    "BS" "Pravoslavni Božić"},
   "11-02"
   {"EN" "All Souls' Day",
    "LT" "Vėlinės",
    "PT" "Dia de Finados",
    "HR" "Dušni dan",
    "HU" "Halottak napja",
    "FIL" "Araw ng mga Kaluluwa",
    "FR" "Fête des morts",
    "DE" "Allerseelen",
    "ET" "hingedepäev",
    "NL" "Allerzielen",
    "VI" "Lễ Các Đẳng",
    "ES" "Día de los Difuntos"},
   "04-01"
   {"EN" "April Fools' Day",
    "HU" "Bolondok napja",
    "NL" "1 April",
    "SQ" "Dita e Gënjeshtrave",
    "VI" "Cá tháng tư"},
   "05-01"
   {"UK" "День міжнародної солідарності трудящих",
    "EN" "Labour Day",
    "IT" "Festa del Lavoro",
    "FI" "Vappu",
    "MK" "Ден на трудот",
    "PAP" "Dia di Obrero",
    "IS" "Hátíðisdagur Verkamanna",
    "LT" "Tarptautinė darbo diena",
    "BG" "Ден на труда",
    "PT" "Dia do trabalhador",
    "HR" "Praznik rada",
    "EL" "Εργατική Πρωτομαγιά",
    "HU" "A munka ünnepe",
    "SR" "Празник рада",
    "FIL" "Araw ng mga Manggagawa",
    "RO" "Ziua muncii",
    "AR" "يوم العمال",
    "TI" "የላብ አደሮች ቀን",
    "ID" "Hari Buruh Internasional",
    "SV" "Första Maj",
    "HY" "Աշխատանքի օր",
    "SQ" "Dita Ndërkombëtare e Punonjësve",
    "EN-US" "Labor Day",
    "FR" "Fête du travail",
    "MS" "Hari Pekerja",
    "MG" "Fetin'ny asa",
    "DE" "Tag der Arbeit",
    "NO" "Arbeidernes dag",
    "ET" "kevadpüha",
    "DA" "1. maj",
    "BE" "Дзень працы",
    "CZ" "Svátek práce",
    "NL" "Dag van de Arbeid",
    "VI" "Quốc tế Lao động",
    "LV" "Darba svētki",
    "BS" "Radni dan",
    "SK" "Sviatok práce",
    "ES" "Día del trabajador",
    "ZH" "劳动节",
    "SL" "Praznik dela",
    "MT" "Jum il-Ħaddiem",
    "PL" "Święto Pracy"},
   "Deepavali" {"EN" "Deepavali", "NL" "Divali"},
   "easter -3"
   {"EN" "Maundy Thursday",
    "IT" "Giovedì santo",
    "IS" "Skírdagur",
    "FIL" "Huwebes Santo",
    "SV" "Skärtorsdagen",
    "KL" "sisamanngortoq illernartoq",
    "FR" "Jeudi saint",
    "DE" "Gründonnerstag",
    "NO" "Skjærtorsdag",
    "DA" "Skærtorsdag",
    "CZ" "Zelený čtvrtek",
    "NL" "Witte donderdag",
    "FO" "Skírhósdagur",
    "VI" "Thứ năm Tuần Thánh",
    "ES" "Jueves Santo"},
   "orthodox"
   {"UK" "Великдень",
    "EN" "Orthodox Easter",
    "MK" "Прв ден Велигден",
    "HR" "Pravoslavni Uskrs",
    "SR" "Васкрс",
    "TI" "ፋሲካ",
    "SQ" "Pashkët Ortodokse",
    "NL" "Orthodox Pasen",
    "BS" "Pravoslavni Vaskrs"},
   "Fathers Day"
   {"EN" "Father's Day",
    "FI" "Isänpäivä",
    "LT" "Tėvo diena",
    "PT" "Dia dos Pais",
    "FR" "Fête des Pères",
    "NO" "Farsdag",
    "ET" "isadepäev",
    "NL" "Vaderdag",
    "VI" "Ngày của cha"},
   "National Holiday"
   {"EN" "National Holiday",
    "DE" "Nationalfeiertag",
    "ES" "Fiesta Nacional",
    "FR" "Fête nationale",
    "HU" "Nemzeti ünnep",
    "EL" "εθνική επέτειος",
    "NL" "Nationale feestdag",
    "VI" "Quốc Lễ"},
   "01-06"
   {"EN" "Epiphany",
    "IT" "Befana",
    "FI" "Loppiainen",
    "AM" "ብርሐነ ጥምቀት",
    "DE-CH" "Dreikönigstag",
    "MK" "Богојавление",
    "IS" "Þrettándinn",
    "HR" "Bogojavljenje, Sveta tri kralja",
    "EL" "Θεοφάνεια",
    "HU" "Vízkereszt",
    "TI" "ጥምቀት",
    "SV" "Trettondedag jul",
    "FR" "l'Épiphanie",
    "DE" "Heilige Drei Könige",
    "ET" "kolmekuningapäev",
    "DA" "Åbenbaring",
    "NL" "Driekoningen",
    "VI" "Lễ Hiển Linh",
    "SK" "Zjavenie Pána",
    "ES" "Día de los Reyes Magos",
    "PL" "Święto Trzech Króli"},
   "10 Tishrei"
   {"EN" "Yom Kippur",
    "BS" "Jom Kipur",
    "DE" "Jom Kippur",
    "HR" "Jom Kipur",
    "MK" "Јом Кипур",
    "NL" "Jom Kipoer",
    "SQ" "Yom Kippur",
    "SR" "Јом Кипур"},
   "orthodox 1"
   {"EN" "Orthodox Easter Monday",
    "MK" "Втор ден Велигден",
    "NL" "Orthodoxe Tweede Paasdag",
    "SR" "Васкрсни понедељак"},
   "12 Rabi al-awwal"
   {"EN" "Birthday of Muhammad (Mawlid)",
    "AM" "መውሊድ",
    "BN" "ঈদে মিলাদুন্নবী",
    "AR" "المولد النبويّ",
    "ID" "Maulid Nabi Muhammad",
    "SQ" "Mevludi",
    "FR" "Mawlid",
    "MS" "Hari Keputeraan Nabi Muhammad S.A.W.",
    "NL" "Mawlid an-Nabi",
    "BS" "Mevlud"},
   "12-31"
   {"EN" "New Year's Eve",
    "IT" "Ultimo dell’anno",
    "FI" "Uudenvuodenaatto",
    "IS" "Gamlársdagur",
    "PT" "Véspera de Ano Novo",
    "HU" "Szilveszter",
    "FIL" "Bisperas ng Bagong Taon",
    "JP" "大晦日",
    "SV" "Nyårsafton",
    "HY" "Նոր տարվա գիշեր",
    "FR" "Saint-Sylvestre",
    "DE" "Silvester",
    "NO" "Nyttårsaften",
    "DA" "Nytårsaften",
    "NL" "Oudejaarsavond",
    "FO" "Nýggjársaftan",
    "VI" "Đêm giao thừa",
    "LV" "Vecgada vakars",
    "ES" "Fin del Año"},
   "Abolition of Slavery"
   {"EN" "Abolition of Slavery",
    "FR" "Abolition de l’esclavage",
    "NL" "Afschaffing van de slavernij",
    "VI" "Bãi bỏ chế độ Nô lệ"},
   "Thaipusam" {"EN" "Thaipusam", "MS" "Hari Thaipusam"},
   "substitutes"
   {"UK" "замінити день",
    "EN" "substitute day",
    "MK" "заменет ден",
    "HR" "zamjena dan",
    "SR" "замена дан",
    "JP" "振替休日",
    "SQ" "ditë zëvendësuese",
    "FR" "jour substitut",
    "DE" "Ersatztag",
    "NL" "substituut",
    "VI" "ngày thay thế",
    "LV" "aizstājējs diena",
    "BS" "zamjena dan",
    "AZ" "əvəz gün",
    "ES" "día sustituto",
    "ZH" "更换日"},
   "Holi" {"EN" "Holi Phagwa", "NL" "Holi-Phagwa"},
   "11-11"
   {"EN" "Saint Martin",
    "DE" "Sankt Martin (Faschingsbeginn)",
    "HU" "Márton nap",
    "NL" "Sint Maarten",
    "VI" "Lễ thánh Martin"},
   "easter -7"
   {"EN" "Palm Sunday",
    "IT" "Domenica delle Palme",
    "IS" "Pálmasunnudagur",
    "HU" "Virágvasárnap",
    "DE" "Palmsonntag",
    "NO" "Palmesøndag",
    "NL" "Palmzondag",
    "VI" "Chúa nhật Lễ Lá",
    "ES" "Domingo de Ramos"},
   "02-02"
   {"EN" "Candlemas",
    "DE" "Lichtmess",
    "HU" "Gyertyaszentelő Boldogasszony",
    "NL" "Lichtmis",
    "VI" "Lễ Đức Mẹ dâng Chúa Giêsu trong đền thánh"},
   "12-25"
   {"DE-AT" "Christtag",
    "EN" "Christmas Day",
    "RU" "Рождество Христово",
    "IT" "Natale",
    "FI" "Joulupäivä",
    "AM" "ልደተ-ለእግዚእነ/ ገና",
    "KO" "기독탄신일",
    "DE-CH" "Weihnachtstag",
    "MK" "Католички Божиќ",
    "PAP" "Dia Pasco di Nascimento",
    "BN" "বড়দিন",
    "IS" "Jóladagur",
    "LT" "Šv. Kalėdos",
    "BG" "Коледа",
    "PT" "Natal",
    "HR" "Božić",
    "EL" "Χριστούγεννα",
    "HU" "Karácsony",
    "SR" "Католички Божић",
    "FIL" "Araw ng Pasko",
    "SW" "Krismasi",
    "JP" "ノエル",
    "RO" "Crăciunul",
    "AR" "عيد الميلاد المجيد",
    "TI" "ልደት",
    "ID" "Hari Raya Natal",
    "SV" "Juldagen",
    "HY" "Սուրբ Ծնունդ",
    "SQ" "Krishtlindja",
    "KL" "juullerujussuaq",
    "FR" "Noël",
    "MS" "Hari Krismas",
    "CA" "Nadal",
    "MG" "Krismasy",
    "DE" "Weihnachten",
    "NO" "Første Juledag",
    "ET" "esimene jõulupüha",
    "DA" "Juledag",
    "CZ" "1. svátek vánoční",
    "NL" "Kerstmis",
    "FO" "Fyrsti jóladagur",
    "VI" "Lễ Giáng Sinh",
    "LV" "Ziemassvētki",
    "BS" "Božić",
    "SK" "Prvý sviatok vianočný",
    "ES" "Navidad",
    "ZH" "聖誕節",
    "SL" "Božič",
    "MT" "Il-Milied",
    "PL" "Pierwszy dzień Bożego Narodzenia"},
   "1 Ramadan"
   {"EN" "First day of Ramadan",
    "AM" "ረመዳን",
    "AR" "اليوم الأول من رمضان",
    "BS" "Prvi dan posta",
    "MS" "Awal Ramadan",
    "NL" "Eerste dag van Ramadan",
    "SQ" "Dita e parë e agjërimit"}})

(defn get-name [id locale]
  (if locale
    (get-in names [id (str/upper-case (name locale))])
    (get-in names [id "EN"])))

(comment
  (get-name "12-25" nil)
  (get-name "12-25" :hr))
