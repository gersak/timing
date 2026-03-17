(ns timing.cron
  (:require
   [clojure.string :as str]
   [timing.core :as core]))

(def ^:dynamic *now*
  {:day 1
   :millisecond 0
   :second 0
   :minute 0
   :hour 0
   :day-in-month 1
   :month 1
   :year 0
   :week 1})

(defn- normalize-day-name
  "Normalizes day names to numbers (1=Mon, 7=Sun).
   Also accepts 0 as Sunday for compatibility."
  [x]
  (let [x (str/lower-case x)]
    (case x
      ("mon" "monday") 1
      ("tue" "tuesday") 2
      ("wed" "wednesday") 3
      ("thu" "thursday") 4
      ("fri" "friday") 5
      ("sat" "saturday") 6
      ("sun" "sunday" "0") 7  ;; 0 is also Sunday
      x)))

(defn- normalize-month-name
  "Normalizes month names to numbers (1-12)."
  [x]
  (let [x (str/lower-case x)]
    (case x
      ("jan" "january") 1
      ("feb" "february") 2
      ("mar" "march") 3
      ("apr" "april") 4
      ("may") 5
      ("jun" "june") 6
      ("jul" "july") 7
      ("aug" "august") 8
      ("sep" "september") 9
      ("oct" "october") 10
      ("nov" "november") 11
      ("dec" "december") 12
      x)))

(defn days-in-month
  "Returns the number of days in a month, accounting for leap years."
  [year month]
  (let [leap-year? (if ((comp not zero?) (mod year 4)) false
                       (if ((comp not zero?) (mod year 100)) true
                           (if ((comp not zero?) (mod year 400)) false
                               true)))]
    (case (int month)
      1 31
      2 (if leap-year? 29 28)
      3 31
      4 30
      5 31
      6 30
      7 31
      8 31
      9 30
      10 31
      11 30
      12 31)))

(defn- day-of-week
  "Returns day of week (1=Mon, 7=Sun) for a given date."
  [year month day]
  (core/day? (core/date->value (core/date year month day))))

(defn- nth-weekday-of-month
  "Returns the day-in-month for the nth occurrence of weekday (1=Mon, 7=Sun).
   Returns nil if there's no such occurrence."
  [year month weekday n]
  (let [first-day (core/date year month 1)
        first-dow (core/day? (core/date->value first-day))
        ;; Days until first occurrence of target weekday
        days-to-first (mod (- weekday first-dow) 7)
        first-occurrence (inc days-to-first)
        ;; Nth occurrence
        target-day (+ first-occurrence (* 7 (dec n)))
        max-day (days-in-month year month)]
    (when (<= target-day max-day)
      target-day)))

(defn- last-weekday-of-month
  "Returns the day-in-month for the last occurrence of weekday (1=Mon, 7=Sun)."
  [year month weekday]
  (let [last-day (days-in-month year month)
        last-dow (day-of-week year month last-day)
        days-back (mod (- last-dow weekday) 7)]
    (- last-day days-back)))

(defn- nearest-weekday
  "Returns the nearest weekday (Mon-Fri) to the given day in month.
   Does not cross month boundaries."
  [year month day]
  (let [max-day (days-in-month year month)
        dow (day-of-week year month day)]
    (case dow
      6 (if (> day 1) (dec day) (+ day 2))      ;; Saturday -> Friday, or Monday if 1st
      7 (if (< day max-day) (inc day) (- day 2)) ;; Sunday -> Monday, or Friday if last
      day)))

(defn- last-weekday-day-of-month
  "Returns the last weekday (Mon-Fri) of the month."
  [year month]
  (let [last-day (days-in-month year month)
        dow (day-of-week year month last-day)]
    (case dow
      6 (- last-day 1)  ;; Saturday -> Friday
      7 (- last-day 2)  ;; Sunday -> Friday
      last-day)))

(defn- parse-special-day-in-month
  "Parses special day-in-month expressions: L, L-n, nW, LW.
   Returns a context-aware validator fn or nil if not a special expression."
  [element]
  (let [elem (str/upper-case element)]
    (cond
      ;; LW - last weekday of month
      (= elem "LW")
      (with-meta
        (fn [value ctx]
          (let [{:keys [year month]} ctx]
            (= value (last-weekday-day-of-month year month))))
        {:cron/context-aware true :cron/type :last-weekday})

      ;; L - last day of month
      (= elem "L")
      (with-meta
        (fn [value ctx]
          (let [{:keys [year month]} ctx]
            (= value (days-in-month year month))))
        {:cron/context-aware true :cron/type :last-day})

      ;; L-n - n days before last day
      (re-matches #"L-(\d+)" elem)
      (let [offset (-> (re-matches #"L-(\d+)" elem)
                       second
                       #?(:clj Integer/parseInt :cljs js/parseInt))]
        (with-meta
          (fn [value ctx]
            (let [{:keys [year month]} ctx
                  last-day (days-in-month year month)]
              (= value (- last-day offset))))
          {:cron/context-aware true :cron/type :last-day-offset :offset offset}))

      ;; nW - nearest weekday to day n
      (re-matches #"(\d+)W" elem)
      (let [day (-> (re-matches #"(\d+)W" elem)
                    second
                    #?(:clj Integer/parseInt :cljs js/parseInt))]
        (with-meta
          (fn [value ctx]
            (let [{:keys [year month]} ctx]
              (= value (nearest-weekday year month day))))
          {:cron/context-aware true :cron/type :nearest-weekday :target-day day}))

      :else nil)))

(defn- parse-special-day-of-week
  "Parses special day-of-week expressions: nL, day#n.
   Returns a context-aware validator fn or nil if not a special expression."
  [element]
  (let [elem (str/upper-case element)]
    (cond
      ;; dayL or nL - last occurrence of weekday in month
      (re-matches #"([A-Z]+|\d)L" elem)
      (let [day-part (second (re-matches #"([A-Z]+|\d)L" elem))
            weekday (if (re-matches #"\d" day-part)
                      #?(:clj (Integer/parseInt day-part) :cljs (js/parseInt day-part))
                      (normalize-day-name day-part))]
        (with-meta
          (fn [value ctx]
            (let [{:keys [year month day-in-month]} ctx
                  last-occ (last-weekday-of-month year month weekday)]
              (and (= value weekday)
                   (= day-in-month last-occ))))
          {:cron/context-aware true :cron/type :last-weekday-occurrence :weekday weekday}))

      ;; day#n - nth occurrence of weekday
      (re-matches #"([A-Z]+|\d)#(\d+)" elem)
      (let [[_ day-part n-part] (re-matches #"([A-Z]+|\d)#(\d+)" elem)
            weekday (if (re-matches #"\d" day-part)
                      #?(:clj (Integer/parseInt day-part) :cljs (js/parseInt day-part))
                      (normalize-day-name day-part))
            n #?(:clj (Integer/parseInt n-part) :cljs (js/parseInt n-part))]
        (when (or (< n 1) (> n 5))
          (throw (ex-info "Occurrence must be between 1 and 5"
                          {:expression element :occurrence n})))
        (with-meta
          (fn [value ctx]
            (let [{:keys [year month day-in-month]} ctx
                  nth-day (nth-weekday-of-month year month weekday n)]
              (and (= value weekday)
                   (some? nth-day)
                   (= day-in-month nth-day))))
          {:cron/context-aware true :cron/type :nth-weekday :weekday weekday :n n}))

      :else nil)))

(defn cron-element-parserer
  "Parses CRON like element. Elements are in form
  1-25/0
  1,5,40/10
  1,20,40
  20-40/5
  */2
  L, L-3, 15W, LW (day-in-month)
  FRI#3, 5L (day-of-week) etc."
  [element [min- max- at]]
  ;; Check for special expressions first
  (if-let [special (case at
                     :day-in-month (parse-special-day-in-month element)
                     :day (parse-special-day-of-week element)
                     nil)]
    special
    ;; Normal parsing
    (letfn [(normalize [x]
              (case at
                :day (normalize-day-name x)
                :month (normalize-month-name x)
                x))
            (parse-number [x]
              #?(:clj (Integer/valueOf (normalize x))
                 :cljs (js/parseInt (normalize x))))]
      (let [[element interval] (str/split element #"/")
            current (get *now* at)
            interval (when interval (parse-number interval))
            fixed (filter
                   #(if (and min- max-)
                      (<= min- % max-)
                      true)
                   (mapv
                    parse-number
                    (remove
                     #(or
                       (re-find #"[a-zA-Z0-9]+-[a-zA-Z0-9]+" %)
                       (re-find #"\*" %)
                       (= % "?"))
                     (str/split element #","))))]
        (when (and
               interval
               (pos? interval)
               (or
                (> interval max-)
                (< interval min-)
                (zero? interval)))
          (throw (ex-info (str "Out of bounds. Interval cannot be outside " [min- max-])
                          {:min min-
                           :max max-
                           :interval interval})))
        (cond
          (and (= element "*") (not interval)) (constantly true)
          (= element "?") (constantly true)
          (= element "*") (set (range min- (inc max-) interval))
          :else (let [ranges (re-seq #"[a-zA-Z0-9]+-[a-zA-Z0-9]+" element)
                      maybe-fixed (cond-> #{}
                                    (seq ranges) (into
                                                  (mapcat
                                                   (fn [r]
                                                     (let [[f l] (str/split r #"-")]
                                                       (range
                                                        (parse-number f)
                                                        (inc (parse-number l)))))
                                                   ranges))
                                    interval (into
                                              (case fixed
                                                ["*"] (range min- (inc max-) interval)
                                                (reduce
                                                 concat
                                                 (map #(range % (inc max-) interval) fixed))))
                                    true (into fixed))]
                  (if (empty? maybe-fixed) (constantly true) maybe-fixed)))))))

(defn normalize-cron-string
  "Normalizes a cron string by right-padding missing fields with '*'.
   - \"*/10\" -> \"*/10 * * * * *\"
   - \"0 */5\" -> \"0 */5 * * * *\"

   Also supports @ shortcuts like @daily, @weekly, etc."
  [^String cron-record]
  (when (or (nil? cron-record) (str/blank? cron-record))
    (throw (ex-info "Cron expression cannot be empty or nil" {:expression cron-record})))
  (let [s (str/trim cron-record)]
    ;; Handle @ shortcuts
    (case (str/lower-case s)
      "@yearly"   "0 0 0 1 1 *"
      "@annually" "0 0 0 1 1 *"
      "@monthly"  "0 0 0 1 * *"
      "@weekly"   "0 0 0 * * 1"
      "@daily"    "0 0 0 * * *"
      "@midnight" "0 0 0 * * *"
      "@hourly"   "0 0 * * * *"
      ;; Default: right-pad with * to reach 6 fields
      (let [elements (mapv str/trim (str/split s #"\s+"))
            n (count elements)
            padded (if (< n 6)
                     (into (vec elements) (repeat (- 6 n) "*"))
                     elements)]
        (str/join " " padded)))))

(def ^:private day-name-pattern
  #"(?i)\b(mon|tue|wed|thu|fri|sat|sun|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b")

(def ^:private month-name-pattern
  #"(?i)\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december)\b")

(defn- validate-field-names
  "Validates that day names are only in day-of-week field and month names only in month field."
  [elements]
  (doseq [[idx element] (map-indexed vector elements)]
    (let [field-name (case idx
                       0 :second
                       1 :minute
                       2 :hour
                       3 :day-in-month
                       4 :month
                       5 :day-of-week
                       :year)]
      ;; Day names only allowed in day-of-week field (idx 5)
      (when (and (not= idx 5) (re-find day-name-pattern element))
        (throw (ex-info (str "Day name found in " (name field-name) " field. Day names (MON-SUN) are only valid in day-of-week field (position 6)")
                        {:field field-name :position (inc idx) :element element})))
      ;; Month names only allowed in month field (idx 4)
      (when (and (not= idx 4) (re-find month-name-pattern element))
        (throw (ex-info (str "Month name found in " (name field-name) " field. Month names (JAN-DEC) are only valid in month field (position 5)")
                        {:field field-name :position (inc idx) :element element}))))))

(defn parse-cron-string
  "Parses CRON string e.g.

  \"0,3,20/20 0 0 3-20/10 * * *\"

  Short forms are auto-padded with '*':
  - \"*/10\" -> \"*/10 * * * * *\" (every 10 seconds)
  - \"0 */5\" -> \"0 */5 * * * *\" (every 5 minutes at second 0)

  Also supports @ shortcuts:
  - @daily, @weekly, @monthly, @yearly, @hourly

  If record is not valid assertion will
  be thrown. Returned data is sequence
  of cron-mappings that define what time
  is valid to execute Job."
  [^String cron-record]
  (let [normalized (normalize-cron-string cron-record)
        elements (mapv str/trim (str/split normalized #"\s+"))]

    ;; Validate day/month names are in correct positions
    (validate-field-names elements)

    (let [constraints [[0 59 :second]
                       [0 59 :minute]
                       [0 23 :hour]
                       [1 31 :day-in-month]
                       [1 12 :month]
                       [1 7 :day]
                       [nil nil]]]

      ;; Pre-validate raw values before parsing
      ;; Skip validation for special expressions (L, W, #)
      (doseq [[element [min-val max-val field-name]] (map vector elements constraints)]
        (when (and min-val max-val
                   (not (re-find #"[LW#]" (str/upper-case element))))
          ;; Check for obvious out-of-range values in the raw element
          (when-let [numbers (re-seq #"\d+" element)]
            (doseq [num-str numbers]
              (let [num #?(:clj (Integer/parseInt num-str)
                           :cljs (js/parseInt num-str))
                    ;; Allow 0 for day-of-week (Sunday)
                    effective-min (if (and (= field-name :day) (= num 0)) 0 min-val)]
                (when (or (< num effective-min) (> num max-val))
                  (throw (ex-info (str "Value " num " is out of range for " field-name " (valid range: " min-val "-" max-val ")")
                                  {:field field-name :value num :min min-val :max max-val :expression cron-record}))))))))

      (mapv cron-element-parserer elements constraints))))

(defn- context-aware?
  "Returns true if validator is context-aware (needs year/month/day context)."
  [validator]
  (-> validator meta :cron/context-aware))

(defn- invoke-validator
  "Invokes a validator with value and optional context.
   Context-aware validators receive both value and context map."
  [validator value ctx]
  (if (context-aware? validator)
    (validator value ctx)
    (validator value)))

(defn- validator->values
  "Extracts valid values from a validator as a sorted vector.
   Returns nil for unbounded validators (constantly true) or context-aware ones."
  [validator min-val max-val]
  (cond
    (nil? validator) nil
    (set? validator) (vec (sort validator))
    (context-aware? validator) nil
    ;; Check if it's (constantly true) by testing bounds
    (and (fn? validator) (validator min-val) (validator max-val)) nil
    (fn? validator) nil
    :else nil))

(defn- compute-day-in-month-values
  "Computes valid day-in-month values for context-aware validators (L, L-n, W, LW)."
  [validator year month]
  (when (context-aware? validator)
    (let [ctx {:year year :month month}
          max-day (days-in-month year month)]
      (filterv #(validator % ctx) (range 1 (inc max-day))))))

(defn- compute-day-of-week-matches
  "Returns days-in-month that match the day-of-week validator for given year/month."
  [validator year month]
  (let [max-day (days-in-month year month)]
    (if (context-aware? validator)
      ;; For FRIL, FRI#3 etc - need full context
      (filterv (fn [d]
                 (let [dow (day-of-week year month d)
                       ctx {:year year :month month :day-in-month d :day-of-week dow}]
                   (validator dow ctx)))
               (range 1 (inc max-day)))
      ;; For simple day-of-week (set or fn)
      (when validator
        (filterv (fn [d]
                   (let [dow (day-of-week year month d)]
                     (validator dow)))
                 (range 1 (inc max-day)))))))

(defn- valid-days-for-month
  "Returns sorted vector of valid days for given year/month based on validators."
  [year month day-in-month-validator day-of-week-validator]
  (let [max-day (days-in-month year month)
        all-days (range 1 (inc max-day))
        ;; Start with days matching day-in-month constraint
        dim-days (cond
                   (nil? day-in-month-validator) all-days
                   (set? day-in-month-validator) (filter #(<= % max-day) day-in-month-validator)
                   (context-aware? day-in-month-validator) (compute-day-in-month-values day-in-month-validator year month)
                   (fn? day-in-month-validator) (filter day-in-month-validator all-days)
                   :else all-days)
        ;; Then filter by day-of-week constraint
        final-days (if (nil? day-of-week-validator)
                     dim-days
                     (if (context-aware? day-of-week-validator)
                       ;; For FRIL, FRI#3 - compute matching days directly
                       (let [dow-days (set (compute-day-of-week-matches day-of-week-validator year month))]
                         (filter dow-days dim-days))
                       ;; For simple DOW validator
                       (filter (fn [d]
                                 (let [dow (day-of-week year month d)]
                                   (day-of-week-validator dow)))
                               dim-days)))]
    (vec (sort final-days))))

(defn- values-from
  "Returns values from sorted coll that are >= start-val, or full coll if start-val is nil."
  [coll start-val]
  (if start-val
    (drop-while #(< % start-val) coll)
    coll))

(defn valid-timestamp?
  "Given a timestamp and cron definition function returns true
   if timestamp satisfies cron definition."
  [timestamp cron-string]
  (let [tv (if (number? timestamp)
             timestamp
             (core/date->value timestamp))
        {:keys [year month day-in-month hour minute second] :as now} (core/day-time-context tv)
        dow (core/day? tv)
        ctx {:year year :month month :day-in-month day-in-month :day-of-week dow}
        elements [second minute hour day-in-month month dow]
        constraints (binding [*now* now] (parse-cron-string cron-string))]
    (every?
     (fn [[validator value]] (invoke-validator validator value ctx))
     (partition 2 (interleave constraints elements)))))

(defn future-timestamps
  [timestamp cron-string]
  (let [timestamp-value (if (number? timestamp)
                          timestamp
                          (core/date->value timestamp))
        {:keys [year month day-in-month hour minute second] :as now} (core/day-time-context timestamp-value)
        mapping (binding [*now* now] (parse-cron-string cron-string))
        ;; Extract validators
        second-validator (get mapping 0)
        minute-validator (get mapping 1)
        hour-validator (get mapping 2)
        day-in-month-validator (get mapping 3)
        month-validator (get mapping 4)
        day-of-week-validator (get mapping 5)
        year-validator (get mapping 6)
        ;; Pre-compute valid values for time fields (not context-dependent)
        valid-seconds (or (validator->values second-validator 0 59) (vec (range 60)))
        valid-minutes (or (validator->values minute-validator 0 59) (vec (range 60)))
        valid-hours (or (validator->values hour-validator 0 23) (vec (range 24)))
        valid-months (or (validator->values month-validator 1 12) (vec (range 1 13)))
        ;; Helper to check if we're past the start timestamp
        after-start? (fn [y m d h min s]
                       (> (core/date->value (core/date y m d h min s))
                          timestamp-value))]
    (for [y (iterate inc year)
          :while (>= y year)
          :when (or (nil? year-validator) (year-validator y))
          m valid-months
          :when (or (> y year) (>= m month))
          :let [valid-days (valid-days-for-month y m day-in-month-validator day-of-week-validator)]
          d valid-days
          :when (or (> y year) (> m month) (>= d day-in-month))
          h valid-hours
          :when (or (> y year) (> m month) (> d day-in-month) (>= h hour))
          min valid-minutes
          :when (or (> y year) (> m month) (> d day-in-month) (> h hour) (>= min minute))
          s valid-seconds
          :when (after-start? y m d h min s)]
      (core/date->value (core/date y m d h min s)))))

(defn next-timestamp
  "Return next valid timestamp after input timestamp. If there is no such timestamp,
  than nil is returned."
  [timestamp cron-string]
  (first (future-timestamps timestamp cron-string)))

(comment
  (time (take 10 (future-timestamps (core/date) "* */10")))
  (time (take 10 (future-timestamps (core/date) "0 */30")))
  (next-timestamp (core/date) "*/10")
  (next-timestamp (core/date) "0 0 0 1 1 * 2018")
  (next-timestamp (core/date) "0 15 9 * * TUE")
  (binding [*now* (-> (core/date) core/time->value core/day-time-context)]
    (parse-cron-string "0 21/3 * * * *"))
  (next-timestamp (core/date 2018 2 9 15 50 30) "*/10")
  (next-timestamp (core/date 2018 2 9 15 50 30) "15 10 13 29 2 4 *")
  (def timestamp (core/date 2018 2 9 15 50 0))
  (def cron-string "0 * * * * *")
  (def tv (core/date->value timestamp))
  (def constraints (parse-cron-string cron-string))
  (valid-timestamp? (core/date 2018 12 20 0 0 0) "0 * * * * * 2019"))
