(ns timing.core
  #?(:cljs (:require-macros [timing.core :refer [with-time-configuration]]))
  (:require #?(:cljs [goog.object])
            clojure.string
            [timing.timezones.db :refer [get-timezone get-rule]])
  (:refer-clojure :exclude [second]))

(def get-locale-timezone timing.timezones.db/get-locale-timezone)
(def get-timezone-locale timing.timezones.db/get-timezone-locale)

(declare utc-date hours minutes calendar-frame month? day-time-context -hour? date)

(defn round-number
  "Function returns round number that is devidable by target-number.
  Rounding strategy can be specified in round-how? options:

  :floor
  :ceil
  :up
  :down

  Rounding number strategy is symetric to 0. This means that :ceil will round
  negative numbers to lower target-number. I.E (round-number -9.5 1 :ceil) would return -10.
  Rounding happens in absolute domain and sign is inserted afterwards.

  Negative target-numbers are not supported. Can't reason about that yet. Maybe for imaginary numbers?"
  ([number] (round-number number 1))
  ([number target-number] (round-number number target-number :down))
  ([number target-number round-how] (round-number number target-number round-how false))
  ([number target-number round-how? symetric?]
   {:pre [(or
            (zero? target-number)
            (pos? target-number))]}
   (case target-number
     0 0
     ;; First normalize numbers to floating point or integer
     (let [round-how? (keyword round-how?)
           diff (rem number target-number)
           base (if (>= target-number 1)
                  (* target-number (quot number target-number))
                  (- number diff))
           limit (* 0.25 target-number target-number)
           compare-fn (if (and (not symetric?) (neg? number))
                        (case round-how?
                          :floor (constantly (not (zero? diff)))
                          :ceil (constantly false)
                          :up <
                          <=)
                        (case round-how?
                          :floor (constantly false)
                          :ceil (constantly (not (zero? diff)))
                          :up <=
                          <))]
       ((if (pos? number) + -)
        base
        (if (compare-fn limit (* diff diff)) target-number 0))))))

(defprotocol TimeValueProtocol
  (time->value [this] "Return numeric value for given object.")
  (value->time [this] "Returns Date for given value."))

(def
  ^{:dynamic true
    :doc "Influences time->value and value->time functions as well as all '?' functions.
         Valid values: 
         :gregorian (default)
         :julian
         :islamic
         :hebrew"}
  *calendar* :gregorian)

(def
  ^{:dynamic true
    :doc "This variable can be used to customize weekend? function to
  return true or false for day-context. Function should accept one
  argument that is supposed to be day [1-7] and return true or false."}
  *weekend-days* #{6 7})

(def
  ^{:dynamic true
    :doc "This variable is supposed to be used through with-time-configuration
  macro. Specify function that calculates if given day-context is
  holiday or not. Look @ \"day-context\" Returns boolean"}
  *holiday?*
  nil)

(def millisecond "1" 1)
(def microsecond "1.0E-3" (/ millisecond 1000))
(def nanosecond "1.0E-6" (/ microsecond 1000))
(def second "1000" (* 1000 millisecond))
(def minute "60000" (* 60 second))
(def hour "3600000" (* 60 minute))
(def day "86400000" (* 24 hour))
(def week "604800000" (* 7 day))

(defn ^:no-doc
  system-timezone []
  #?(;:clj (.. (java.util.TimeZone/getDefault) (getID))
     :clj (str (java.time.ZoneId/systemDefault))
     :cljs (goog.object/get (.resolvedOptions (js/Intl.DateTimeFormat)) "timeZone")))

(def
  ^{:dynamic true
    :doc "Variable that is used in function get-dst-offset. ->local, <-local and day? funcitons are affected
   when changing value of this variable. I.E. (binding [*timezone* :hr] ...) would make all computations
   in that time zone (offset)."}
  *timezone*
  (system-timezone))

(def
  ^{:dynamic true
    :doc "Variable that is used in function get-dst-offset. ->local, <-local and day? funcitons are affected
   when changing value of this variable. I.E. (binding [*offset* (hours -2)] ...) would make all computations
   in that offset from UTC."}
  *offset* nil)

(defn unknown-holiday [_] false)

(defmacro with-time-configuration
  "Utility macro to put context frame on computation scope. Specify:

  :holiday?     - (fn [day-context] true | false)
  :timezone     - timezone-name
  :weekend-days - (fn [number] true | false)
  :calendar     - :gregorian, :julian, :islamic, :hebrew

  WARNING: Lazy sequences may be realized outside this binding scope.
  Use (doall ...) or (vec ...) to force evaluation within the binding:

    (with-time-configuration {:timezone \"UTC\"}
      (vec (map day-time-context values)))  ; Force evaluation here"
  [{:keys [timezone
           holiday?
           offset
           calendar
           weekend-days]
    :or {weekend-days timing.core/*weekend-days*
         timezone (timing.core/system-timezone)
         holiday? *holiday?*
         calendar :gregorian
         offset nil}}
   & body]
  `(binding [timing.core/*timezone* ~timezone
             timing.core/*offset* ~offset
             timing.core/*weekend-days* ~weekend-days
             timing.core/*holiday?* ~holiday?
             timing.core/*calendar* ~calendar]
     ~@body))

(defmacro with-utc
  "Convenience macro for UTC timezone context. Equivalent to:
  (with-time-configuration {:timezone \"UTC\" :offset 0} ...)"
  [& body]
  `(with-time-configuration {:timezone "UTC" :offset 0}
     ~@body))

;; TODO - enable this to conform with history rules not just current one
;; This is + on lib size and speed. I've compiled TZ with history and it
;; is about 0.5 Mb in size. That is alot for CLJS not that much for CLJ
;; For now SUPPORT ONLY LATEST(CURRENT) TZ DB (72 KB)

(defn- get-timezone-attribute [timezone attribute]
  (get (get-timezone timezone) attribute))

(defn- get-timezone-offset [timezone]
  (get-timezone-attribute timezone :offset))

(defn- get-timezone-rule [timezone]
  (when-let [rule (get-timezone-attribute timezone :rule)]
    (when-let [rule-name (case rule
                           "-" nil
                           rule)]
      (get-rule rule-name))))

(defn value->utc-date
  "Casts value to UTC value of Date object"
  [value]
  #?(:cljs (js/Date. value)
     :clj (java.util.Date. value)))

(defn date->utc-value
  "Returns number of milliseconds from UNIX epoch"
  [date]
  (.getTime date))

(defn ^:no-doc until-value
  [{:keys [year month day-in-month floating-day]
    :as until
    :or {month 1
         day-in-month 1}}]
  ;; Find calendar frame for this month
  (when until
    (let [days-mapping
          {"Mon" 1
           "Tue" 2
           "Wed" 3
           "Thu" 4
           "Fri" 5
           "Sat" 6
           "Sun" 7}
          {:keys [hour minute]
           :or {hour 0
                minute 0}} (:time until)]
      (if floating-day
        (if (clojure.string/starts-with? floating-day "last")
          ;; Floating day is last something
          (let [day' (days-mapping (subs floating-day 4))
                value (:value
                        (last
                          (filter
                            #(= day' (:day %))
                            (->
                              (utc-date year month)
                              date->utc-value
                              (calendar-frame :month)))))]
            value
            (+ value (minutes minute) (hours hour)))
          ;; Floating day is higher than
          (let [day' (days-mapping (subs floating-day 0 3))
                operator-def (subs floating-day 3 5)
                operator (case operator-def
                           ">=" >=
                           "<=" <=)
                day-in-month' #?(:clj (Integer/parseInt (subs floating-day 5))
                                 :cljs (js/parseInt (subs floating-day 5)))
                limit (date->utc-value (utc-date year month day-in-month'))
                value (:value
                        (first
                          (filter
                            #(and
                               (= day' (:day %))
                               (operator (:value %) limit))
                            (take 100
                                  (map day-time-context
                                       (iterate
                                         (fn [v]
                                           ((case operator-def
                                              ">=" +
                                              "<=" -)
                                            v
                                            day))
                                         limit))))))]
            (+ value (hours hour) (minutes minute))))
        (date->utc-value (utc-date year month day-in-month hour minute))))))

(defn get-dst-offset
  "Returns DST offset for UTC input value"
  [value]
  (letfn [(utc-rule? [rule] (if-not (= "s" (-> rule :time :time-suffix)) true false))]
    (if (nil? *timezone*)
      (throw (ex-info "No timezone defined" {:value value}))
      (binding [*holiday?* nil]
        (if-let [{dst-rule :daylight-savings
                  s-rule :standard
                  ;; TODO - if this is going to expand to other rules in history
                  ;;  than get-timezone-rule should be function of two arguments
                  ;; *timezone* and value
                  ;; THIS IS ONLY PLACE REQUIRED FOR EXPANDING TO FULL TZ SUPPORT
                  :as rule} (get-timezone-rule *timezone*)]
          (let [month (binding [*offset* 0] (month? value))]
            (if-not dst-rule 0
                    (if (< (:month dst-rule) (:month s-rule))
                      ;; Northen hemisphere
                      (cond
                        ;; Standard time use timezone offset
                        (and
                          (< month (:month s-rule))
                          (> month (:month dst-rule))) (:save dst-rule)
                        (or
                          (< month (:month dst-rule))
                          (> month (:month s-rule))) 0
                        :else
                        (let [month-frame (calendar-frame value "month")
                              save-light? (= month (:month dst-rule))
                              limit (+
                                      (if-not save-light?
                                        (:save dst-rule)
                                        0)
                                      (binding [*offset* 0]
                                        (until-value
                                          (assoc
                                            (if save-light?
                                              dst-rule
                                              s-rule)
                                            :year (:year (first month-frame))))))
                              save (:save dst-rule)
                              hour (-hour? value)
                              save-offset (if ((if save-light? < >=) value limit)
                                            0
                                            save)
                              jump-offset (if ((if save-light? < >=) (+ value save) limit)
                                            0
                                            save)]
                          save-offset))
                      ;; Southern hemisphere
                      (cond
                        ;; Standard time use timezone offset
                        (and
                          (> month (:month s-rule))
                          (< month (:month dst-rule))) 0
                        (or
                          (> month (:month dst-rule))
                          (< month (:month s-rule))) (:save dst-rule)
                        :else
                        (let [month-frame (calendar-frame value "month")
                              save-light? (= month (:month dst-rule))
                              limit (+
                                      (if-not save-light?
                                        (:save dst-rule)
                                        0)
                                      (binding [*offset* 0]
                                        (until-value
                                          (assoc
                                            (if save-light?
                                              dst-rule
                                              s-rule)
                                            :year (:year (first month-frame))))))]
                          (if ((if save-light? < >=) value limit)
                            0
                            (:save dst-rule)))))))
          0)))))

(comment
  (def value (-> (date 2023 3 26 1 1) date->value (+ hour) value->date))
  (value->utc-date value)
  ;; BUG - this doesn't work properly... When value 
  ;; is in DST, day-time-context won't provide proper hour
  ;; value!
  (-> (date 2023 3 26 1 1) date->value (+ hour) day-time-context)
  ;; This works but it isn't used in calendar-frame
  (-> (date 2023 3 26 1 1) date->value (+ hour) day-context)
  ())

(defn ^:no-doc <-local
  "Given a local timestamp value function normalizes datetime to Greenwich timezone value"
  ([value] (<-local
             value
             (if (number? *offset*) *offset*
                 (if (string? *timezone*)
                   (let [tz-offset (get-timezone-offset *timezone*)]
                    ;; HERE.... i should check if it is leap hour
                    ;; or minute or whatever and than apply dst offset
                    ;; properly
                     (+
                     ;; move to standard time
                       (get-dst-offset value)
                       tz-offset))
                  ;; Fallback to deprecated getTimeZoneOffset
                   (* -1 (minutes (.getTimezoneOffset (value->utc-date value))))))))
  ([value offset] (+ value offset)))

(defn ^:no-doc ->local
  "Given a Greenwich timestamp value function normalizes datetime to local timezone value"
  ([value]
   (->local
     value
     (if (number? *offset*)
       *offset*
       (if (string? *timezone*)
         (let [tz-offset (get-timezone-offset *timezone*)
               dst-current (get-dst-offset (- value tz-offset))
               dst-before (get-dst-offset (- value tz-offset hour))
               dst-offset (if (= dst-before dst-current)
                            dst-current
                            dst-before)]
           (+
           ;; remove timezone offset to move to standard time 
             dst-offset
             tz-offset))
                ;; Fallback to deprecated getTimeZoneOffset
         (* -1 (minutes (.getTimezoneOffset (value->utc-date value))))))))
  ([value offset] (- value offset)))

(defn milliseconds
  "Function returns value of n seconds as number."
  [n]
  (* n millisecond))

(defn seconds
  "Function returns value of n seconds as number."
  [n]
  (* n second))

(defn minutes
  "Function returns value of n minutes as number."
  [n]
  (* n minute))

(defn hours
  "Function returns value of n hours as number."
  [n]
  (* n hour))

(defn days
  "Function returns value of n days as number."
  [n]
  (* n day))

(defn weeks
  "Function returns value of n weeks as number."
  [n]
  (* 7 n day))

(defn midnight
  "Function calculates value of midnight for given value. For example
  if some date value is inputed it will round-number to the begining of
  that day."
  [value]
  (round-number value day :floor))

(defn before-midnight
  ([value] (before-midnight value false))
  ([value strict?]
   (- (+ (midnight value) day)
      (if strict? nanosecond second))))

(def ^:no-doc julian-day-epoch
  (with-time-configuration
    {:offset 0}
    (- (days 2440588))))

;; Credits 
;; Algorithms taken from awesome Astronomy Answers page
;; https://www.aa.quae.nl/en/reken/juliaansedag.html

(defn ^:no-doc value->jcdn
  "For given value returns which Julian Day value belongs to"
  [value]
  (/ (- (round-number value day :floor) julian-day-epoch) day))

(defn ^:no-doc jcdn->value [jcdn]
  (+ (days jcdn) julian-day-epoch))

(defn ^:no-doc value->gregorian-date [value]
  (letfn [(div [x y]
            [(quot x y)
             (mod x y)])]
    (let [J (value->jcdn value)
          [x3 r3] (div (- (* 4 J) 6884477) 146097)
          [x2 r2] (div
                    (+
                      (* 100 (round-number (/ r3 4) 1 :floor))
                      99)
                    36525)
          [x1 r1] (div
                    (+ (* 5 (round-number (/ r2 100) 1 :floor)) 2)
                    153)
          d (inc (round-number (/ r1 5) 1 :floor))
          c0 (round-number (/ (+ x1 2) 12) 1 :floor)
          j (long (+ (* 100 x3) x2 c0))
          m (long (+ x1 (* -12 c0) 3))
          leap-year? (if ((comp not zero?) (mod j 4)) false
                         (if ((comp not zero?) (mod j 100)) true
                             (if ((comp not zero?) (mod j 400)) false
                                 true)))
          days-in-month (case m
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
                          12 31)]
      {:day-in-month (long d)
       :month m
       :year j
       :leap-year? leap-year?
       :days-in-month days-in-month
       :days-in-year (if leap-year? 366 365)
       :first-day-in-month? (== 1 d)
       :last-day-in-month? (== days-in-month d)})))

(defn ^:no-doc gregorian-date->value [{:keys [day-in-month month year]
                                       :or {month 1
                                            day-in-month 1}}]
  (let [c0 (round-number (/ (- month 3) 12) 1 :floor)
        x4 (+ year c0)
        x3 (quot x4 100)
        x2 (rem x4 100)
        x1 (- month (* 12 c0) 3)
        J (+
            (round-number (/ (* 146097 x3) 4) 1 :floor)
            (round-number (/ (* 36525 x2) 100) 1 :floor)
            (round-number (/ (+ 2 (* 153 x1)) 5) 1 :floor)
            day-in-month
            1721119)]
    (jcdn->value J)))

(defn ^:no-doc value->julian-date [value]
  (let [J (value->jcdn value)
        y2 (- J 1721118)
        k2 (+ (* 4 y2) 3)
        k1 (+ (* 5 (round-number (/ (mod k2 1461) 4) 1 :floor)) 2)
        x1 (round-number (/ k1 153) 1 :floor)
        c0 (round-number (/ (+ x1 2) 12) 1 :floor)
        j (+ (round-number (/ k2 1461) 1 :floor) c0)
        m (+ x1 (* c0 -12) 3)
        d (inc (round-number (/ (mod k1 153) 5) 1 :floor))
        leap-year? (if ((comp not zero?) (mod j 4)) false true)
        days-in-month (case m
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
                        12 31)]
    {:day-in-month (long d)
     :month (long m)
     :year (long j)
     :leap-year? leap-year?
     :days-in-year (if leap-year? 366 365)
     :first-day-in-month? (== 1 d)
     :days-in-month days-in-month
     :last-day-in-month? (== d days-in-month)}))

(defn ^:no-doc julian-date->value [{:keys [day-in-month month year]
                                    :or {month 1
                                         day-in-month 1}}]
  (let [J0 1721117
        c0 (round-number (/ (- month 3) 12) 1 :floor)
        J1 (round-number
             (/ (* 1461 (+ year c0)) 4)
             1
             :floor)
        J2 (round-number
             (/ (- (* 153 month) (* 1836 c0) 457) 5)
             1
             :floor)]
    (jcdn->value (+ J1 J2 day-in-month J0))))

;;  Type   r   J0
;;  Ia     15  1948439
;;  Ic     15  1948440
;;  IIa    14  1948439
;;  IIc    14  1948440
;;  IIIa   11  1948439
;;  IIIc   11  1948440
;;  IVa    9   1948439
;;  IVc    9   1948440

(defn ^:no-doc value->islamic-date [value]
  (letfn [(div [x y]
            [(quot x y)
             (mod x y)])]
    (let [J0 1948440
          r 14
          J (value->jcdn value)
          y2 (- J J0)
          [x2 r2] (div
                    (-
                      (+ (* 30 y2) 29)
                      r)
                    10631)
          z2 (round-number (/ r2 30) 1 :floor)
          [x1 r1] (div (+ (* 11 z2) 5) 325)
          z1 (round-number (/ r1 11) 1 :floor)
          j (inc x2)
          m (inc x1)
          d (inc z1)
          value' (+ value (days (- 30 d)))
          J' (value->jcdn value')
          y2' (- J' J0)
          [x2' r2'] (div
                      (-
                        (+ (* 30 y2') 29)
                        r)
                      10631)
          z2' (round-number (/ r2' 30) 1 :floor)
          [x1' r1'] (div (+ (* 11 z2') 5) 325)
          m' (inc x1')
          days-in-month (if (== m m') 30 29)
          z1' (round-number (/ r1' 11) 1 :floor)
          d' (inc z1')
          year-in-cycle (mod j 30)
          leap-year? (case year-in-cycle
                       (2, 5, 7, 10, 13, 15, 18, 21, 24, 26, 29) true
                       false)]
      {:day-in-month (long d)
       :month (long m)
       :year (long j)
       :days-in-year (if leap-year? 355 354)
       :days-in-month days-in-month
       :first-day-in-month? (== 1 d)
       :last-day-in-month? (== days-in-month d)})))

(defn ^:no-doc islamic-date->value [{:keys [day-in-month month year]
                                     :or {month 1
                                          day-in-month 1}}]
  (let [J0 1948440
        r 14]
    (jcdn->value
      (+
        (round-number
          (/ (+ (- (* 10631 year) 10631) r) 30)
          1
          :floor)
        (round-number
          (/ (- (* 325 month) 320) 11)
          1
          :floor)
        day-in-month
        (dec J0)))))

(defn- ^:no-doc hebrew-c1 [x]
  (round-number
    (/ (inc (* 235 x)) 19)
    1 :floor))

; (defn- ^:no-doc hebrew-q [x]
;   (round-number
;     (/ (hebrew-c1 x) 1095)
;     1 :floor))

; (defn- ^:no-doc hebrew-r [x] (mod (hebrew-c1 x) 1095))

(defn- ^:no-doc hebrew-v1 [x]
  (+
    (* 29 (hebrew-c1 x))
    (round-number (/ (+ 12084 (* 13753 (hebrew-c1 x))) 25920) 1 :floor)))

(defn- ^:no-doc hebrew-v2 [x]
  (+
    (hebrew-v1 x)
    (mod
      (round-number
        (/ (* 6 (mod (hebrew-v1 x) 7)) 7)
        1 :floor)
      2)))

(defn- ^:no-doc hebrew-L2 [x] (- (hebrew-v2 (inc x)) (hebrew-v2 x)))

(defn- ^:no-doc hebrew-v3 [x]
  (* 2
     (mod
       (round-number (/ (+ (hebrew-L2 x) 19) 15) 1 :floor)
       2)))

(defn- ^:no-doc hebrew-v4 [x]
  (mod
    (round-number (/ (+ 7 (hebrew-L2 (dec x))) 15) 1 :floor)
    2))

(defn- ^:no-doc hebrew-c2 [x] (+ (hebrew-v2 x) (hebrew-v3 x) (hebrew-v4 x)))
(defn- ^:no-doc hebrew-L [x] (- (hebrew-c2 (inc x)) (hebrew-c2 x)))
(defn- ^:no-doc hebrew-c8 [x] (mod (round-number (/ (+ 7 (hebrew-L x)) 2) 1 :floor) 15))
(defn- ^:no-doc hebrew-c9 [x] (* -1 (mod (round-number (/ (- 385 (hebrew-L x)) 2) 1 :floor) 15)))

(defn- ^:no-doc hebrew-c3 [x m]
  (+
    (round-number
      (/ (+ 7 (* 384 m)) 13)
      1 :floor)
    (*
      (hebrew-c8 x)
      (round-number (/ (+ m 4) 12) 1 :floor))
    (*
      (hebrew-c9 x)
      (round-number (/ (+ m 3) 12) 1 :floor))))

(defn- ^:no-doc hebrew-c4 [x m] (+ (hebrew-c2 x) (hebrew-c3 x m)))

(defn hebrew-date->value
  [{:keys [day-in-month month year]
    :or {month 1
         day-in-month 1}}]
  (let [c0 (round-number
             (/ (- 13 month) 7)
             1 :floor)
        x1 (+ c0 (dec year))
        z4 (dec day-in-month)
        J (+
            347821
            (hebrew-c4 x1 (dec month))
            z4)]
    (jcdn->value J)))
;;
(defn ^:no-doc value->hebrew-date
  [value]
  (let [J (value->jcdn value)
        y4 (- J 347821)
        q (round-number (/ y4 1447) 1 :floor)
        r (mod y4 1447)
        y1' (+ (* 49 q) (round-number (/ (+ (* 23 q) (* 25920 r) 13835)
                                         765433)
                                      1 :floor))
        gama1 (inc y1')
        epsilon1 (round-number (/ (+ (* 19 gama1) 17)
                                  235)
                               1 :floor)
        u1 (- gama1 (round-number (/ (inc (* 235 epsilon1)) 19) 1 :floor))
        c41' (hebrew-c4 epsilon1 u1)
        ro1 (- y4 c41')
        gama2 (+ gama1 (round-number (/ ro1 33) 1 :floor))
        epsilon2 (round-number (/ (+ (* 19 gama2) 17)
                                  235)
                               1 :floor)
        u2 (- gama2 (round-number (/ (inc (* 235 epsilon2)) 19) 1 :floor))
        c42' (hebrew-c4 epsilon2 u2)
        ro2 (- y4 c42')
        gama3 (+ gama2 (round-number (/ ro2 33) 1 :floor))
        epsilon3 (round-number (/ (+ (* 19 gama3) 17)
                                  235)
                               1 :floor)
        u3 (- gama3 (round-number (/ (inc (* 235 epsilon3)) 19) 1 :floor))
        z4 (- y4 (hebrew-c4 epsilon3 u3))
        c (round-number (/ (- 12 u3) 7) 1 :floor)
        j (long (- (inc epsilon3) c))
        m (long (inc u3))
        d (long (inc z4))
        year-length (long (hebrew-L epsilon3))
        month-mapping (case year-length
                        353 (zipmap (range 1 13)
                                    [30 29 30 29 30 29 30 29 29 29 30 29])
                        354 (zipmap (range 1 13)
                                    [30 29 30 29 30 29 30 29 30 29 30 29])
                        355 (zipmap (range 1 13)
                                    [30 29 30 29 30 29 30 30 30 29 30 29])
                        383 (zipmap (range 1 14)
                                    [30 29 30 29 30 29 30 30 30 29 30 30 29])
                        384 (zipmap (range 1 13)
                                    [30 29 30 29 30 29 30 29 30 29 30 30 29])
                        385 (zipmap (range 1 13)
                                    [30 29 30 29 30 29 30 30 30 29 30 30 29]))
        last-day (get month-mapping m)]
    {:year j
     :month m
     :day-in-month d
     :year-lenght year-length
     :days-in-month last-day
     :days-in-year year-length
     :first-day-in-month? (= 1 d)
     :last-day-in-month? (= last-day d)}))

(defn millisecond?
  "Returns which millisecond in day does input value belongs to. For example
  for date 15.02.2015 it will return number 0"
  [value]
  (mod value 1000))

(defn second?
  "Returns which second in day does input value belongs to. For example
  for date 15.02.2015 it will return number 0"
  [value]
  (long
    (mod
      (/ (round-number value second :floor) second)
      60)))

(defn minute?
  "Returns which hour in day does input value belongs to. For example
  for date 15.02.2015 it will return number 0"
  [value]
  (long
    (mod
      (/ (round-number value minute :floor) minute)
      60)))

(defn- -hour?
  "Returns which hour in day does input value belongs to. For example
  for date 15.02.2015 it will return number 0"
  [value]
  (long
    (mod
      (/ (round-number value hour :floor) hour)
      24)))

(defn hour?
  "Returns which hour in day does input value belongs to. For example
  for date 15.02.2015 it will return number 0"
  [value]
  (let [raw-hour (-hour? value)
        {{{dst :hour} :time} :daylight-savings} (get-timezone-rule *timezone*)]
    (if (nil? dst)
      raw-hour
      (let [offset (quot (get-timezone-offset *timezone*) hour)
            dst (+ dst offset)]
        (if (= raw-hour dst)
          (let [dst-before (get-dst-offset (- value (hours dst)))
                dst-current (get-dst-offset value)]
            (if (and (= dst-before 0) (> dst-current 0))
              (+ raw-hour (quot dst-current hour))
              raw-hour))
          raw-hour)))))

(defn- ^:no-doc date-map->value
  [{:keys [hour minute second millisecond]
    :or {hour 0
         minute 0
         second 0
         millisecond 0}
    :as date}]
  (let [_time (reduce + [(hours hour) (minutes minute) (seconds second) (milliseconds millisecond)])
        date-value ((case *calendar*
                      :gregorian gregorian-date->value
                      :julian julian-date->value
                      :islamic islamic-date->value
                      :hebrew hebrew-date->value)
                    date)]
    (long (+ date-value _time))))

(defn- ^:no-doc value->date-map
  [value]
  (assoc
    ((case *calendar*
       :gregorian value->gregorian-date
       :julian value->julian-date
       :islamic value->islamic-date
       :hebrew value->hebrew-date)
     value)
    :hour (-hour? value)
    :minute (minute? value)
    :second (second? value)
    :millisecond (millisecond? value)))

(defn day?
  "Returns which day in week does input value belongs to. For example
   for date 15.02.2015 it will return number 7"
  [value]
  (let [move-days (/ (round-number value day (if (neg? value) :ceil :floor)) day)]
    (inc (mod (+ 3 move-days) 7))))

(defn weekend?
  "Returns true if value in seconds belongs to *weekend-days*"
  [value]
  (boolean (*weekend-days* (day? value))))

(defn year?
  "For given value year? returns year that value belogs to."
  [value]
  (:year (value->date-map value)))

(defn value-in-year?
  "Returns value relative to current year"
  [value]
  (let [start-year-value (date-map->value {:year (year? value)})]
    (- value start-year-value)))

(defn day-in-year?
  "Returns day in year period (1 - 366)"
  [value]
  (long
    (quot
      (value-in-year? value)
      day)))

(defn value-in-month?
  "Returns value relative to current month"
  [value]
  (let [start-month-value (date-map->value {:year (year? value)
                                            :month (month? value)})]
    (- value start-month-value)))

(defn week-in-year?
  "Returns which week in year does input value belongs to. For example
  for date 15.02.2015 it will return number 6"
  [value]
  (let [year-start (date-map->value {:year (year? value)})
        value (midnight value)
        first-monday (first
                       (filter
                         #(#{1} (day? %))
                         (iterate (partial + day) year-start)))
        time-difference (- value first-monday)
        week-in-year (round-number time-difference week :floor)]
    (if (== first-monday year-start)
      (long (+ (/ week-in-year week) 1))
      (inc (long (+ (/ week-in-year week) 1))))))

(defn month?
  "Returns which month does input value belongs to. For example
  for date 15.02.2015 it will return number 2"
  [value]
  (:month (value->date-map value)))

(defn first-day-in-month
  "Returns value of first day in current month"
  [value])

(defn last-day-in-month
  "Returns value of last day in current month"
  [value])

(defn day-in-month?
  "Returns which day (Gregorian) in month input value belongs to. For example
  for date 15.02.2015 it will return number 15"
  [value]
  (:day-in-month (value->date-map value)))

(defn first-day-in-month?
  "Returns true if value in seconds belongs to first day in month."
  [value]
  (= 1 (day-in-month? value)))

(defn last-day-in-month?
  "Returns true if value in seconds belongs to last day in month."
  [value]
  (:last-day-in-month? (value->date-map value)))

(defn utc-date-value
  "Computes value for given arguments depending with respect to *calendar* binding"
  ([] (date->utc-value
        #?(:cljs (js/Date.)
           :clj (java.util.Date.))))
  ([year] (utc-date-value year 1))
  ([year month] (utc-date-value year month 1))
  ([year month day] (utc-date-value year month day 0))
  ([year month day hour] (utc-date-value year month day hour 0))
  ([year month day hour minute] (utc-date-value year month day hour minute 0))
  ([year month day hour minute second] (utc-date-value year month day hour minute second 0))
  ([year month day' hour' minute' second' millisecond']
   (date-map->value
     {:year year
      :month month
      :day-in-month day'
      :hour hour'
      :minute minute'
      :second second'
      :millisecond millisecond'})))

(def utc-date (comp value->utc-date utc-date-value))

(defn date
  "Constructs new Date object"
  ([]
   (utc-date))
  ([year] (date year 1))
  ([year month] (date year month 1))
  ([year month day] (date year month day 0))
  ([year month day hour] (date year month day hour 0))
  ([year month day hour minute] (date year month day hour minute 0))
  ([year month day hour minute second] (date year month day hour minute second 0))
  ([year month day' hour' minute' second' millisecond']
   (let [value (utc-date-value year month day' hour' minute' second' millisecond')]
     (value->utc-date (->local value)))))

(defn period
  "Returns number of seconds of all input arguments added together"
  ([{:keys [weeks days hours seconds minutes milliseconds]
     :or {weeks 0
          days 0
          hours 0
          seconds 0
          minutes 0
          milliseconds 0}}]
   (reduce + 0
           [(* 7 day weeks)
            (* day days)
            (* hour hours)
            (* second seconds)
            (* minutes minute)
            (* milliseconds millisecond)])))

(defn period?
  "Returns duration of for input value in form of map with keys:

   :weeks
   :days
   :hours
   :minutes
   :seconds
   :milliseconds"
  [value]
  (letfn [(round-period [value limit]
            (let [r (round-number value limit :floor)]
              [(- value r) (int (quot r limit))]))]
    (let [[r w] (round-period value week)
          [r d] (round-period r day)
          [r h] (round-period r hour)
          [r m] (round-period r minute)
          [r s] (round-period r second)
          [r ms] (round-period r millisecond)]
      {:weeks w
       :hours h
       :days d
       :minutes m
       :seconds s
       :milliseconds ms})))

(defn value->date
  "Returns Date instance for value in seconds for current. Function first
  transforms value to local *timezone* value."
  ([value]
   (let [value (->local value)]
     (new
       #?(:clj java.util.Date
          :cljs js/Date)
       (long value)))))

(defn date->value
  "Returns value of Date instance in seconds. Value is localized to offset"
  ([t]
   (when t (<-local (date->utc-value t)))))

(defn context->value
  "Function turns map of time units to value"
  [{:keys [year
           month
           day-in-month
           hour
           minute
           second
           millisecond]
    :or {year 1970
         month 1
         day-in-month 1
         hour 0
         minute 0
         second 0
         millisecond 0}}]
  (date->value (date year month day-in-month hour minute second millisecond)))

#?(:clj
   (extend-protocol TimeValueProtocol
     java.lang.Long
     (value->time [this] (value->date this))
     (time->value [this] this)
     java.lang.Integer
     (value->time [this] (value->date this))
     (time->value [this] this)
     java.lang.Number
     (value->time [this] (value->date this))
     (time->value [this] this)
     java.lang.Float
     (value->time [this] (value->date this))
     (time->value [this] this)
     java.lang.Double
     (value->time [this] (value->date this))
     (time->value [this] this)
     java.math.BigInteger
     (value->time [this] (value->date this))
     (time->value [this] this)
     java.math.BigDecimal
     (value->time [this] (value->date this))
     (time->value [this] this)
     clojure.lang.BigInt
     (value->time [this] (value->date this))
     (time->value [this] this)
     clojure.lang.Ratio
     (value->time [this] (value->date this))
     (time->value [this] this)
     clojure.lang.ISeq
     (value->time [this] (map value->time this))
     (time->value [this] (map time->value this))
     clojure.lang.IPersistentMap
     (time->value [this] (context->value this))
     clojure.lang.IPersistentVector
     (value->time [this] (mapv value->date this))
     (time->value [this] (mapv time->value this))
     clojure.lang.IPersistentSet
     (value->time [this] (set (map value->date this)))
     (time->value [this] (set (map time->value this)))

     java.util.Date
     (time->value [this] (date->value this))
     (value->time [this] this)
     java.time.Instant
     (value->time [this] (java.util.Date/from this))
     (time->value [this] (date->value (java.util.Date/from this))))
   :cljs
   (extend-protocol TimeValueProtocol
     number
     (value->time [this] (value->date this))
     (time->value [this] this)
     js/Date
     (time->value [this] (date->value this))
     (value->time [this] this)
     cljs.core/PersistentArrayMap
     (value->time [this] this)
     (time->value [this] (context->value this))
     cljs.core/PersistentHashMap
     (value->time [this] this)
     (time->value [this] (context->value this))
     cljs.core/PersistentVector
     (value->time [this] (mapv value->time this))
     (time->value [this] (mapv time->value this))
     cljs.core/PersistentHashSet
     (value->time [this] (set (map value->time this)))
     (time->value [this] (set (map time->value this)))))

(defn intervals
  "Given sequence of timestamps (Date) values returns period values between each timestamp
  value in milliseconds"
  [& timestamps]
  (assert
    (every? #(satisfies? TimeValueProtocol %) timestamps)
    (str "Wrong input value."))
  (let [timestamps' (map time->value timestamps)
        t1 (rest timestamps')
        t2 (butlast timestamps')]
    (map - t1 t2)))

(defn interval
  "Returns period of time value in milliseconds between start and end. Input values
  are supposed to be Date."
  [start end]
  (first (intervals start end)))

(defn teleport
  "Teleports value to different timezone."
  [value timezone]
  (let [d (value->time value)]
    (with-time-configuration
      {:timezone timezone}
      (time->value d))))

(defn before
  "Returns Date object that was before some time value"
  [date value]
  (->
    date
    time->value
    (- value)
    value->time))

(defn after
  "Returns Date object that was after some time value"
  [date value]
  (->
    date
    time->value
    (+ value)
    value->time))

;; TIME FRAMES
(defmulti calendar-frame
  "Returns sequence of days for given value that are contained in that frame-type. List is consisted
  of keys:

  :value
  :day
  :week
  :month
  :day-in-month
  :weekend?
  :holiday?
  :first-day-in-month?
  :last-day-in-month?

  for Gregorian calendar. Frame types can be extened by implementing different calendar-frame functions.
  Timing supports calendar-frames for:
  * year
  * month
  * week"
  (fn [value frame-type & options] frame-type))

(defn day-time-context
  "Returns day context for given value in Gregorian calendar.

  Returnes hash-map with keys:
    :value
    :millisecond
    :second
    :hour
    :day
    :week
    :month
    :year
    :day-in-month
    :weekend?
    :holiday?
    :first-day-in-month?
    :last-day-in-month?

  Note: For DST-accurate hour values, use day-context instead."
  [value]
  (let [day (day? value)
        context (value->date-map value)]
    (as-> context context
      (assoc
        context
        :value value
        :day day
        :weekend? (boolean (*weekend-days* day))
        :week (week-in-year? value))
      (assoc
        context
        :holiday? (if (fn? *holiday?*)
                    (*holiday?* context)
                    false)))))

(defn day-context
  "Returns day context with DST-accurate hour value.
   Use this instead of day-time-context when you need accurate :hour during DST transitions."
  [value]
  (assoc (day-time-context value)
         :hour (hour? value)))

(defn calendar-period-context
  "Computes interval statistics based on start Date and end Date
  * start
  * end
  * start-context
  * end-context
  * years
  * months
  * days
  * hours
  * minutes
  * seconds
  * holidays
  * weekends"
  ([start end] (calendar-period-context start end <))
  ([start end comparator]
   (let [start (time->value start)
         end (time->value end)
         delta day
         marks (conj
                 (vec
                   (take-while
                     #(comparator % end)
                     (iterate #(+ % delta) start)))
                 end)
         milliseconds (- end start)
         seconds (/ milliseconds second)
         minutes (/ seconds 60)
         hours (/ minutes 60)
         days (/ hours 24)
         contexts (map day-time-context marks)
         {:keys [weekends in/days holidays]
          :as result} (update
                        (reduce
                          (fn [r {:keys [day day-in-month month year]
                                  :as context}]
                            (let [weekend? (and (ifn? *weekend-days*) (*weekend-days* day))]
                              (cond->
                                (->
                                  r
                                  (update :months conj [year month])
                                  (update :years conj year))
                                ;;
                                (and (ifn? *holiday?*) (*holiday?* context))
                                (update :holidays conj [year month day-in-month weekend?])
                                ;;
                                weekend?
                                (update :weekends conj [year month day-in-month]))))
                          {:start start
                           :end end
                           :months (sorted-set)
                           :years (sorted-set)
                           :holidays (sorted-set)
                           :weekends #{}
                           :in/days days
                           :in/hours hours
                           :in/minutes minutes
                           :in/seconds seconds
                           :in/milliseconds milliseconds}
                          contexts)
                        :weekends count)
         ;;
         {start-year-days :days-in-year
          :as start-context} (first contexts)
         {end-year-days :days-in-year
          :as end-context} (last contexts)
         from-start-year-value (value-in-year? start)
         to-end-year-value (value-in-year? end)
         ;;
         result
         (assoc result
           :in/years
           (let [{:keys [years]} result
                 year-count (count years)
                 from-start-year-part
                 (if-not (> year-count 1) 0
                         (/
                           (- start-year-days (/ from-start-year-value day))
                           start-year-days))
                      ;;
                 to-end-year-part
                 (if-not (> year-count 1) 0
                         (/
                           (/ to-end-year-value day)
                           end-year-days))
                      ;;
                 same-year-part
                 (if (not= year-count 1) 0
                     (/
                       (/ (- end start) day)
                       start-year-days))]
             (+
               (count (drop 2 years))
               from-start-year-part
               to-end-year-part
               same-year-part))
                ;;
           :in/months
           (let [{:keys [months]} result
                 start-month-value (context->value (dissoc start-context :year :month))
                 end-month-value (context->value (dissoc end-context :year :month))
                 month-count (count months)
                 start-month-part (if-not (> month-count 1) 0
                                          (/
                                            (- (:days-in-month start-context) (/ start-month-value day))
                                            (:days-in-month start-context)))
                 end-month-part (if-not (> month-count 1) 0
                                        (/ (/ end-month-value day) (:days-in-month end-context)))
                 same-month-part (if-not (= 1 month-count) 0
                                         (/ (- end-month-value start-month-value)
                                            day
                                            (:days-in-month start-context)))]
             (+ (count (drop 2 months)) start-month-part end-month-part same-month-part))
                ;;
           :working-days
           (max
             (-
               days weekends
               (count
                 (filter
                   (fn [[_ _ w]] (nil? w))
                   holidays)))
             0))]
     #?(:clj (reduce-kv
               (fn [r k _]
                 (case (namespace k)
                   "in" (update r k double)
                   r))
               result
               result)
        :cljs result))))

(defmethod calendar-frame :year [value _]
  (let [{:keys [days-in-month year]} (day-time-context value)
        first-day (time->value (date year 1 1))]
    (loop [cd first-day
           m 1
           r []]
      (if (> m 12) r
          (recur
            (+ cd (days days-in-month))
            (inc m)
            (concat r (calendar-frame cd :month)))))))

(defmethod calendar-frame "year" [value _]
  (calendar-frame value :year))

(defmethod calendar-frame :month [value _]
  (let [{:keys [year month days-in-month
                day-in-month]} (day-time-context value)
        current-day (midnight value)
        first-day (-
                    current-day
                    (days (dec day-in-month)))
        first-week (week-in-year? first-day)
        first-day-in-week (day? first-day)]
    (for [d (range days-in-month)
          :let [v (+ first-day (days d))
                day (mod (+ first-day-in-week d) 7)
                day (if (zero? day) 7 day)
                week' (+
                        first-week
                        (quot
                          (dec (+ d first-day-in-week))
                          7))]]

      {:value v
       :month month
       :year year
       :week week'
       :day day
       :day-in-month (inc d)
       :first-day-in-month? (= (inc d) 1)
       :last-day-in-month? (= d (dec days-in-month))
       :weekend (boolean (*weekend-days* day))})))

(defmethod calendar-frame "month" [value _]
  (calendar-frame value :month))

(defmethod calendar-frame :week [value _]
  (let [{:keys [day]} (day-time-context value)
        current-day (midnight value)
        first-day (- current-day (days (dec day)))]
    (for [d (range 0 7)
          :let [v (+ first-day (days d))]]
      (day-time-context v))))

(defmethod calendar-frame "week" [value _]
  (calendar-frame value :week))

#?(:clj

   (defmacro time-as-value
     "bindings => [name (time->value x) ...]

     Similar to let or binding. Casts all bound symbol values with
     function time->value. Then evaluates body."
     [bindings & body]
     (assert (vector? bindings) "Bindings should be vector")
     (assert (even? (count bindings)) "Odd count of bindings. Try to even binding count.")
     (cond
       (= (count bindings) 0) `(do ~@body)
       (symbol? (bindings 0)) (let [bindings (reduce
                                               (fn [r [s v]]
                                                 (conj r s (list 'timing.core/time->value v)))
                                               []
                                               (partition 2 bindings))]
                                `(let ~bindings
                                   ~@body))
       :else (throw
               #?(:clj
                  (IllegalArgumentException.
                    "time-as-value only allows Symbols in bindings")
                  :cljs
                  (js/Error.

                    "time-as-value allows only Symbols in bindings"))))))

(comment
  (time (count (take 100 (map day-time-context (iterate #(+ day %) (time->value (date)))))))
  (+ 1 1)
  (time (time->value (date 2019 1 7)))
  (time
    (clojure.pprint/pprint
      (let [value (time->value (date 2019 1 7))]
        (with-time-configuration {:calendar :julian}
          (calendar-frame value :month)))))
  (time
    (let [value (time->value (date 2019 1 7))]
      (with-time-configuration {:calendar :julian}
        (day-time-context value))))
  (def date2 (time->value (date 1582 10 15)))
  (= date1 date2))
