(ns timing.holiday.compile
  (:require
   clojure.pprint
   [clojure.string :as str]
   [yaml.core :as yaml]
   [timing.timezones.db :refer [locales]]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.walk :as walk]))

(def +target+ "generated/timing/holiday/")

(defn read-locale-holidays
  [locale]
  (yaml/parse-string
   (slurp
    (format
     "https://raw.githubusercontent.com/commenthol/date-holidays/master/data/countries/%s.yaml"
     (str/upper-case (name locale))))
   :keywords false))

(defn get-holiday-days
  "Function task locale input and reads yaml file from date-holidays
  master branch. Result is parsed yaml data with path ['holidays' 'locale' 'days']"
  ([locale]
   (try
     (get-holiday-days (read-locale-holidays locale) locale)
     (catch Throwable _ nil)))
  ([yaml locale]
   (when yaml
     (get-in yaml ["holidays"
                   (str/upper-case (name locale))
                   "days"]))))

(defn get-holidays
  ([locale]
   (keys (get-holiday-days locale)))
  ([yaml locale]
   (keys (get-holiday-days yaml locale))))

(defn get-holiday-details
  ([locale holiday]
   (get-in (get-holiday-days locale) [holiday]))
  ([yaml locale holiday]
   (get-in (get-holiday-days yaml locale) [holiday])))

(defn get-holidays-identifiers
  ([locale]
   (remove nil?
           (for [val (vals (get-holiday-days locale))]
             (get-in val ["_name"]))))
  ([yaml locale]
   (remove nil?
           (for [val (vals (get-holiday-days yaml locale))]
             (get-in val ["_name"])))))

(defn get-holiday-types
  ([locale]
   (remove nil?
           (distinct
            (for [val (vals (get-holiday-days locale))]
              (get-in val ["type"])))))
  ([yaml locale]
   (remove nil?
           (distinct
            (for [val (vals (get-holiday-days yaml locale))]
              (get-in val ["type"]))))))

(defn get-locale-types [locale]
  (try
    (set (map #(get % "type") (vals (get-holiday-days locale))))
    (catch Throwable _ #{})))

(defn get-all-types
  []
  (reduce into #{} (map get-locale-types (keys locales))))

(defn get-all-holidays
  []
  (reduce into (sorted-set) (map get-holidays (keys locales))))

(def holiday-types {nil "observance" "optional" "observence" "public" "Observance" "bank" "school"})

(defn analyze-holiday [text]
  (condp re-find text
    #"^\d{4}-\d{2}-\d{2}$" :static
    #"^\d{2}-\d{2}$" :static_reocurring
    #"^\d{2}-\d{2}.*" :static_condition
    #"^julian.*" :julian
    #".* (before|after) .*" :dayofweek_before_after
    #"^\d[a-zA-Z]{2}.*" :dayofweek_in_month
    #"^easter.*" :easter
    #"^orthodox.*" :orthodox
    nil))

(comment
  (def text "02-08")
  (def text "03-03 and if saturday,sunday then next monday")
  (def text "1st Monday in June")
  (def text "Tuesday after 1st Monday in August")
  (def text "Monday before 03-08")
  (def text "easter")
  (def text "easter 40")
  (def text "easter -47")
  (def text "orthodox")
  (def text "orthodox 39")
  (def text "orthodox -3")
  (def text "2039-07-15")
  (def locale :hr)
  (def locale :mk)
  (def locale :no)
  (def holiday "easter 1")
  (def holiday "12-25")
  (def holiday "05-30")
  (spit "all_holidays.edn" (with-out-str (clojure.pprint/pprint (get-all-holidays))))
  (def holidays (clojure.edn/read-string (slurp "all_holidays.edn")))
  (def report (group-by analyze-holiday holidays))
  (spit
   "unknown_holidays.edn"
   (with-out-str
     (clojure.pprint/pprint (get report nil))))

  (keys locales)
  (keys (get-holiday-days :hr))
  (set (map #(get % "type") (vals (get-holiday-days :hr))))
  ;; (read-holiday-names)
  ((vals (get-holiday-days :hr)))
  (slurp "https://raw.githubusercontent.com/commenthol/date-holidays/master/data/countries")
  (time (read-locale-holidays :hr)))

(defn parse-names
  [holidays]
  (loop [[hday & hdays] holidays
         result nil]
    (if (nil? hday) result
        (condp #(and (map? (val hday)) (contains? %2 %1)) (val hday)
          "name"
          (let [name-part (walk/keywordize-keys (get (val hday) "name"))
                add-name-keyword (assoc (val hday) :name name-part)
                remove-old-name (dissoc add-name-keyword "name")]
            (recur hdays (assoc result (key hday) remove-old-name)))
          ;;
          "_name"
          (let [name-part (edn/read-string
                           {:read-cond :preserve}
                           (str "(partial get-name " "\"" (get (val hday) "_name") "\"" ")"))
                add-name-keyword (assoc (val hday) :name name-part)
                remove-old-name (dissoc add-name-keyword "_name")]
            (recur hdays (assoc result (key hday) remove-old-name)))
          ;;
          (recur hdays result)))))

(defn escape-quotes-in-map [m]
  (clojure.walk/postwalk
   (fn [x]
     (if (string? x)
       (clojure.string/escape x {\" "\\\""})
       x))
   m))

(defn generate-locale-holidays [locale]
  (let [f (slurp (io/file "resources/locale_holidays.template"))
        holiday-data (-> (get-holiday-days locale)
                         parse-names
                         escape-quotes-in-map)]
    (->
     f
     (clojure.string/replace #"<<locale>>" (name locale))
     (clojure.string/replace #"<<holidays>>"
                             (binding [clojure.core/*print-length* nil]
                               (with-out-str
                                 (clojure.pprint/pprint holiday-data)))))))

#_(defn generate-locale-holidays [locale]
  ;;   (slurp (io/resource "locale_holidays.template"))
    (let [f (slurp (io/file "resources/locale_holidays.template"))]
      (->
       f
       (clojure.string/replace
        #"<<locale>>"
        (name locale))
       (clojure.string/replace
        #"<<holidays>>"
        (with-out-str
          (binding [clojure.core/*print-length* nil]
            (clojure.pprint/pprint
             (parse-names (get-holiday-days locale)))))))))

(comment
  (def locale :il)
  (get-holiday-days :il))

#_(defn -main []
    (map #(spit (str +target+ % ".cljc") (generate-locale-holidays (keyword %))) (map str/lower-case (keys locales))))

(defn generate-all-namespace
  []
  (spit (str +target+ "all.cljc")
        (let [locales (map str/lower-case (keys locales))]
          (str
           "(ns timing.holiday.all\n"
           " (:require\n  "
           (str/join "\n  " (for [locale locales]
                              (symbol (str "[timing.holiday." locale "]"))))
           "))"))))

(defn -main []
  (clojure.java.io/make-parents (str +target+ "dummy.txt"))
  (doseq [local (map str/lower-case (keys locales))]
    (spit (str +target+ local ".cljc") (generate-locale-holidays (keyword local))))
  (generate-all-namespace))

(comment
  (-main)
  (keys locales)
  (clojure.java.io/make-parents (str +target+ "hr" ".cljc"))
  (spit (str +target+ "hr" ".cljc") (generate-locale-holidays :hr))
  (spit (str +target+ "il" ".cljc") (generate-locale-holidays :il))
  (def h (timing.holidays/holiday? (timing.core/date 2022 05 05) :hr))
  (timing.holidays/name h :hr)
  (count (map str/lower-case (keys locales)))
  (timing.holidays.hr/holiday? (timing.core/date 2022 12 25)))
