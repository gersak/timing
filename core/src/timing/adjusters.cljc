(ns timing.adjusters
  "Temporal adjusters for the Timing library - functions that adjust dates/times
  to specific temporal points using numeric domain arithmetic."
  (:require [timing.core :as core]))

;; Basic temporal adjusters

(defn next-day-of-week
  "Adjusts value to the next occurrence of the specified day of week.
  Day: 1=Monday, 2=Tuesday, ..., 7=Sunday"
  [value target-day]
  (let [current-day (core/day? value)
        days-to-add (mod (- target-day current-day) 7)]
    (+ value (core/days (if (zero? days-to-add) 7 days-to-add)))))

(defn previous-day-of-week
  "Adjusts value to the previous occurrence of the specified day of week.
  Day: 1=Monday, 2=Tuesday, ..., 7=Sunday"
  [value target-day]
  (let [current-day (core/day? value)
        days-to-subtract (mod (- current-day target-day) 7)]
    (- value (core/days (if (zero? days-to-subtract) 7 days-to-subtract)))))

(defn next-or-same-day-of-week
  "Adjusts value to the next occurrence of the specified day of week,
  or returns the same value if it's already that day."
  [value target-day]
  (let [current-day (core/day? value)]
    (if (= current-day target-day)
      value
      (next-day-of-week value target-day))))

(defn start-of-week
  "Adjusts value to the start of the week (Monday by default).
  Options: {:first-day-of-week 7} for Sunday start"
  ([value] (start-of-week value {}))
  ([value {:keys [first-day-of-week] :or {first-day-of-week 1}}]
   (let [current-day (core/day? value)
         days-back (mod (- current-day first-day-of-week) 7)]
     (core/midnight (- value (core/days days-back))))))

(defn end-of-week
  "Adjusts value to the end of the week (Sunday by default).
  Options: {:first-day-of-week 7} for Sunday start"
  ([value] (end-of-week value {}))
  ([value {:keys [first-day-of-week] :or {first-day-of-week 1}}]
   (let [last-day (if (= first-day-of-week 1) 7 6)]
     (core/before-midnight (next-or-same-day-of-week value last-day)))))

(defn start-of-month
  "Adjusts value to the first day of the month at midnight."
  [value]
  (let [{:keys [year month]} (core/day-time-context value)]
    (core/time->value (core/date year month 1))))

(defn end-of-month
  "Adjusts value to the last day of the month just before midnight."
  [value]
  (let [{:keys [year month]} (core/day-time-context value)
        next-month-start (if (= month 12)
                           (core/time->value (core/date (inc year) 1 1))
                           (core/time->value (core/date year (inc month) 1)))]
    (- next-month-start 1)))

(defn start-of-year
  "Adjusts value to January 1st at midnight."
  [value]
  (let [{:keys [year]} (core/day-time-context value)]
    (core/time->value (core/date year 1 1))))

(defn end-of-year
  "Adjusts value to December 31st just before midnight."
  [value]
  (let [{:keys [year]} (core/day-time-context value)]
    (- (core/time->value (core/date (inc year) 1 1)) 1)))

(defn first-day-of-month-on-day-of-week
  "Finds the first occurrence of a day of week in the month.
  Example: First Monday of March 2024"
  [value target-day]
  (let [month-start (start-of-month value)
        first-target (next-or-same-day-of-week month-start target-day)]
    (if (= (core/month? first-target) (core/month? value))
      first-target
      (+ first-target (core/days 7)))))

(defn last-day-of-month-on-day-of-week
  "Finds the last occurrence of a day of week in the month.
  Example: Last Friday of March 2024"
  [value target-day]
  (let [month-end (end-of-month value)
        month-end-day (core/midnight month-end)
        last-target (previous-day-of-week (+ month-end-day (core/days 1)) target-day)]
    last-target))

(defn nth-day-of-month-on-day-of-week
  "Finds the nth occurrence of a day of week in the month.
  Example: 3rd Tuesday of March 2024"
  [value target-day n]
  (let [first-occurrence (first-day-of-month-on-day-of-week value target-day)]
    (+ first-occurrence (core/days (* 7 (dec n))))))

;; Business day adjusters

(defn next-business-day
  "Adjusts to the next business day (Monday-Friday).
  Options: {:weekend-days #{6 7}} to customize weekend"
  ([value] (next-business-day value {}))
  ([value {:keys [weekend-days] :or {weekend-days #{6 7}}}]
   (loop [candidate (+ value (core/days 1))]
     (if (weekend-days (core/day? candidate))
       (recur (+ candidate (core/days 1)))
       candidate))))

(defn previous-business-day
  "Adjusts to the previous business day (Monday-Friday).
  Options: {:weekend-days #{6 7}} to customize weekend"
  ([value] (previous-business-day value {}))
  ([value {:keys [weekend-days] :or {weekend-days #{6 7}}}]
   (loop [candidate (- value (core/days 1))]
     (if (weekend-days (core/day? candidate))
       (recur (- candidate (core/days 1)))
       candidate))))

;; Quarter adjusters

(defn start-of-quarter
  "Adjusts value to the first day of the quarter."
  [value]
  (let [{:keys [year month]} (core/day-time-context value)
        quarter-start-month (case (quot (dec month) 3)
                              0 1 ; Q1: Jan-Mar
                              1 4 ; Q2: Apr-Jun  
                              2 7 ; Q3: Jul-Sep
                              3 10 ; Q4: Oct-Dec
                              )]
    (core/time->value (core/date year quarter-start-month 1))))

(defn end-of-quarter
  "Adjusts value to the last day of the quarter."
  [value]
  (let [{:keys [year month]} (core/day-time-context value)
        quarter-end-month (case (quot (dec month) 3)
                            0 3 ; Q1: Jan-Mar
                            1 6 ; Q2: Apr-Jun
                            2 9 ; Q3: Jul-Sep  
                            3 12 ; Q4: Oct-Dec
                            )]
    (end-of-month (core/time->value (core/date year quarter-end-month 1)))))

;; Composite adjusters - these show the power of functional composition

(defn add-business-days
  "Adds n business days, skipping weekends."
  ([value n] (add-business-days value n {}))
  ([value n {:keys [weekend-days] :or {weekend-days #{6 7}}}]
   (if (zero? n)
     value
     (let [direction (if (pos? n) 1 -1)
           abs-n (Math/abs n)]
       (loop [current value
              remaining abs-n]
         (if (zero? remaining)
           current
           (let [next-day (+ current (core/days direction))]
             (if (weekend-days (core/day? next-day))
               (recur next-day remaining)
               (recur next-day (dec remaining))))))))))

(defn add-months
  "Adds n months to a time value, handling variable month lengths properly.
  Examples:
  - Jan 31 + 1 month = Feb 28 (Feb has fewer days)
  - Feb 29 + 12 months = Feb 28 (if target year is not a leap year)
  - Any date + n months maintains the same day when possible"
  [value n]
  (let [{:keys [year month day-in-month hour minute second millisecond]}
        (core/day-time-context value)
        target-year (+ year (quot (+ month n -1) 12))
        target-month (inc (mod (+ month n -1) 12))
        ;; Get the last day of target month to handle overflow
        target-month-start (core/time->value (core/date target-year target-month 1))
        target-month-end-value (let [next-month-start (if (= target-month 12)
                                                        (core/time->value (core/date (inc target-year) 1 1))
                                                        (core/time->value (core/date target-year (inc target-month) 1)))]
                                 (- next-month-start 1))
        target-month-end-context (core/day-time-context target-month-end-value)
        ;; Clamp day to valid range
        target-day (min day-in-month (:day-in-month target-month-end-context))]
    (core/time->value (core/date target-year target-month target-day hour minute second millisecond))))

(defn add-years
  "Adds n years to a time value, handling leap years properly.
  Examples:
  - Feb 29, 2024 + 1 year = Feb 28, 2025 (2025 is not a leap year)
  - Any other date + n years maintains the same month and day"
  [value n]
  (let [{:keys [year month day-in-month hour minute second millisecond]}
        (core/day-time-context value)
        target-year (+ year n)
        ;; Handle Feb 29 on non-leap years by checking if target date would be valid
        target-day (if (and (= month 2) (= day-in-month 29))
                     ;; For Feb 29, check if target year is a leap year
                     (let [target-context (core/value->gregorian-date
                                           (core/gregorian-date->value
                                            {:year target-year :month 2 :day-in-month 1}))]
                       (if (:leap-year? target-context) 29 28))
                     day-in-month)]
    (core/time->value (core/date target-year month target-day hour minute second millisecond))))

;; Utility functions for common patterns

(defn every-nth-day-of-week
  "Generates a sequence of every nth occurrence of a day-of-week.
  Example: Every 2nd Monday starting from a date"
  [start-value target-day n]
  (let [first-occurrence (next-or-same-day-of-week start-value target-day)]
    (iterate #(+ % (core/days (* 7 n))) first-occurrence)))

(defn business-days-in-range
  "Returns sequence of business days between start and end (inclusive)."
  ([start end] (business-days-in-range start end {}))
  ([start end {:keys [weekend-days] :or {weekend-days #{6 7}}}]
   (->> (range start (inc end) (core/days 1))
        (remove #(weekend-days (core/day? %))))))

(comment
  (->> (range 0 12 3)
       (map #(add-months (core/time->value (core/date 2024 1 1)) %))
       (map core/value->time))

  (require '[timing.core :as t])
  (let [now (t/date)]
    (println
     (t/with-time-configuration {:calendar :hebrew}
       (select-keys (t/day-time-context (t/time->value now)) [:year :month :day-in-month])))
    (println
     (t/with-time-configuration {:calendar :islamic}
       (select-keys (t/day-time-context (t/time->value now)) [:year :month :day-in-month]))))

  (def q1 (-> (core/date 2024 1 15) core/time->value))
  (->> (business-days-in-range (start-of-quarter q1) (end-of-quarter q1))
       (take 10)
       (map core/value->time))

  (require '[timing.holiday :as holiday])
  (require '[timing.holiday.all])

  (holiday/name :hr (holiday/? :us (t/time->value (t/date 2024 7 4))))
  (holiday/name :en (holiday/? :us (t/time->value (t/date 2024 12 25))))
  (holiday/? :us (t/time->value (t/date 2024 1 1)))
  (holiday/name :hr (holiday/? :us (t/time->value (t/date 2024 1 1))))

  (def today (t/time->value (t/date)))
  (def next-friday
    (-> today
        (t/midnight)
        (next-day-of-week 5)
        (+ (t/hours 17))))  ; 5 PM

  (println "Next Friday at 5 PM:" (t/value->time next-friday)))
