(ns timing.adjusters-test
  (:refer-clojure :exclude [second])
  (:use clojure.test
        timing.core)
  (:require [timing.adjusters :as adj]))

(deftest test-next-day-of-week
  (testing "next-day-of-week adjustments"
    (let [friday (time->value (date 2024 3 15))] ; Friday March 15, 2024
      ;; Friday to next Monday
      (is (= 1 (day? (adj/next-day-of-week friday 1))))
      (is (= 18 (:day-in-month (day-time-context (adj/next-day-of-week friday 1)))))

      ;; Friday to next Friday (should be 7 days later)
      (is (= 5 (day? (adj/next-day-of-week friday 5))))
      (is (= 22 (:day-in-month (day-time-context (adj/next-day-of-week friday 5)))))

      ;; Friday to next Sunday
      (is (= 7 (day? (adj/next-day-of-week friday 7))))
      (is (= 17 (:day-in-month (day-time-context (adj/next-day-of-week friday 7))))))))

(deftest test-previous-day-of-week
  (testing "previous-day-of-week adjustments"
    (let [friday (time->value (date 2024 3 15))] ; Friday March 15, 2024
      ;; Friday to previous Monday
      (is (= 1 (day? (adj/previous-day-of-week friday 1))))
      (is (= 11 (:day-in-month (day-time-context (adj/previous-day-of-week friday 1)))))

      ;; Friday to previous Friday (should be 7 days earlier)
      (is (= 5 (day? (adj/previous-day-of-week friday 5))))
      (is (= 8 (:day-in-month (day-time-context (adj/previous-day-of-week friday 5)))))

      ;; Friday to previous Sunday
      (is (= 7 (day? (adj/previous-day-of-week friday 7))))
      (is (= 10 (:day-in-month (day-time-context (adj/previous-day-of-week friday 7))))))))

(deftest test-next-or-same-day-of-week
  (testing "next-or-same-day-of-week adjustments"
    (let [friday (time->value (date 2024 3 15))] ; Friday March 15, 2024
      ;; Friday to same Friday (should return same value)
      (is (= friday (adj/next-or-same-day-of-week friday 5)))

      ;; Friday to next Monday
      (is (= 1 (day? (adj/next-or-same-day-of-week friday 1))))
      (is (= 18 (:day-in-month (day-time-context (adj/next-or-same-day-of-week friday 1))))))))

(deftest test-start-end-of-week
  (testing "start and end of week adjustments"
    (let [friday (time->value (date 2024 3 15 14 30 0))] ; Friday March 15, 2024 at 2:30 PM
      ;; Start of week (Monday) - should be midnight of Monday
      (let [week-start (adj/start-of-week friday)]
        (is (= 1 (day? week-start))) ; Monday
        (is (= 11 (:day-in-month (day-time-context week-start))))
        (is (= 0 (:hour (day-time-context week-start))))
        (is (= 0 (:minute (day-time-context week-start)))))

      ;; End of week (Sunday) - should be just before midnight of Sunday
      (let [week-end (adj/end-of-week friday)]
        (is (= 7 (day? week-end))) ; Sunday
        (is (= 17 (:day-in-month (day-time-context week-end))))
        (is (= 23 (:hour (day-time-context week-end))))
        (is (= 59 (:minute (day-time-context week-end)))))

      ;; Custom first day of week (Sunday start)
      (let [week-start-sunday (adj/start-of-week friday {:first-day-of-week 7})]
        (is (= 7 (day? week-start-sunday))) ; Sunday
        (is (= 10 (:day-in-month (day-time-context week-start-sunday))))))))

(deftest test-start-end-of-month
  (testing "start and end of month adjustments"
    (let [mid-march (time->value (date 2024 3 15 14 30 0))] ; March 15, 2024
      ;; Start of month - should be March 1st at midnight
      (let [month-start (adj/start-of-month mid-march)]
        (is (= 1 (:day-in-month (day-time-context month-start))))
        (is (= 3 (:month (day-time-context month-start))))
        (is (= 0 (:hour (day-time-context month-start)))))

      ;; End of month - should be March 31st just before midnight
      (let [month-end (adj/end-of-month mid-march)]
        (is (= 31 (:day-in-month (day-time-context month-end))))
        (is (= 3 (:month (day-time-context month-end))))
        (is (= 23 (:hour (day-time-context month-end)))))

      ;; Test February in leap year
      (let [feb-leap (time->value (date 2024 2 15))
            feb-end (adj/end-of-month feb-leap)]
        (is (= 29 (:day-in-month (day-time-context feb-end)))))

      ;; Test February in non-leap year
      (let [feb-normal (time->value (date 2023 2 15))
            feb-end (adj/end-of-month feb-normal)]
        (is (= 28 (:day-in-month (day-time-context feb-end))))))))

(deftest test-business-days
  (testing "business day adjustments"
    (let [friday (time->value (date 2024 3 15))] ; Friday March 15, 2024
      ;; Next business day from Friday should be Monday
      (let [next-bday (adj/next-business-day friday)]
        (is (= 1 (day? next-bday))) ; Monday
        (is (= 18 (:day-in-month (day-time-context next-bday)))))

      ;; Previous business day from Friday should be Thursday
      (let [prev-bday (adj/previous-business-day friday)]
        (is (= 4 (day? prev-bday))) ; Thursday
        (is (= 14 (:day-in-month (day-time-context prev-bday)))))

      ;; Adding business days
      (let [plus-5-bdays (adj/add-business-days friday 5)]
        (is (= 5 (day? plus-5-bdays))) ; Friday (skips weekend)
        (is (= 22 (:day-in-month (day-time-context plus-5-bdays)))))

      ;; Subtracting business days
      (let [minus-5-bdays (adj/add-business-days friday -5)]
        (is (= 5 (day? minus-5-bdays))) ; Friday
        (is (= 8 (:day-in-month (day-time-context minus-5-bdays)))))

      ;; Zero business days should return same value
      (is (= friday (adj/add-business-days friday 0))))))

(deftest test-add-months-years
  (testing "month and year addition with proper handling of edge cases"
    ;; Normal month addition
    (let [jan-15 (time->value (date 2024 1 15))]
      (let [feb-15 (adj/add-months jan-15 1)]
        (is (= 2 (:month (day-time-context feb-15))))
        (is (= 15 (:day-in-month (day-time-context feb-15)))))

      ;; Year addition
      (let [next-year (adj/add-years jan-15 1)]
        (is (= 2025 (:year (day-time-context next-year))))
        (is (= 1 (:month (day-time-context next-year))))
        (is (= 15 (:day-in-month (day-time-context next-year))))))

    ;; Edge case: Jan 31 + 1 month should become Feb 28/29
    (let [jan-31 (time->value (date 2024 1 31))
          feb-result (adj/add-months jan-31 1)]
      (is (= 2 (:month (day-time-context feb-result))))
      (is (= 29 (:day-in-month (day-time-context feb-result))))) ; 2024 is leap year

    ;; Edge case: Feb 29 + 1 year should become Feb 28 in non-leap year
    (let [feb-29-2024 (time->value (date 2024 2 29))
          feb-28-2025 (adj/add-years feb-29-2024 1)]
      (is (= 2025 (:year (day-time-context feb-28-2025))))
      (is (= 2 (:month (day-time-context feb-28-2025))))
      (is (= 28 (:day-in-month (day-time-context feb-28-2025)))))

    ;; Multiple months across year boundary
    (let [nov-15 (time->value (date 2024 11 15))
          feb-15-next (adj/add-months nov-15 3)]
      (is (= 2025 (:year (day-time-context feb-15-next))))
      (is (= 2 (:month (day-time-context feb-15-next))))
      (is (= 15 (:day-in-month (day-time-context feb-15-next)))))))

(deftest test-nth-day-of-month-adjusters
  (testing "first, last, and nth day of month adjusters"
    (let [march-2024 (time->value (date 2024 3 15))] ; March 2024
      ;; First Monday of March 2024 should be March 4th
      (let [first-monday (adj/first-day-of-month-on-day-of-week march-2024 1)]
        (is (= 1 (day? first-monday)))
        (is (= 4 (:day-in-month (day-time-context first-monday)))))

      ;; Last Friday of March 2024 should be March 29th
      (let [last-friday (adj/last-day-of-month-on-day-of-week march-2024 5)]
        (is (= 5 (day? last-friday)))
        (is (= 29 (:day-in-month (day-time-context last-friday)))))

      ;; Third Tuesday of March 2024 should be March 19th
      (let [third-tuesday (adj/nth-day-of-month-on-day-of-week march-2024 2 3)]
        (is (= 2 (day? third-tuesday)))
        (is (= 19 (:day-in-month (day-time-context third-tuesday)))))

      ;; First occurrence (n=1) should match first-day-of-month function
      (let [first-wed-method1 (adj/first-day-of-month-on-day-of-week march-2024 3)
            first-wed-method2 (adj/nth-day-of-month-on-day-of-week march-2024 3 1)]
        (is (= first-wed-method1 first-wed-method2))))))

(deftest test-quarter-adjusters
  (testing "quarter start and end adjusters"
    ;; Q1 test (March)
    (let [march-15 (time->value (date 2024 3 15))]
      (let [q1-start (adj/start-of-quarter march-15)]
        (is (= 1 (:month (day-time-context q1-start))))
        (is (= 1 (:day-in-month (day-time-context q1-start)))))
      (let [q1-end (adj/end-of-quarter march-15)]
        (is (= 3 (:month (day-time-context q1-end))))
        (is (= 31 (:day-in-month (day-time-context q1-end))))))

    ;; Q2 test (June)
    (let [june-15 (time->value (date 2024 6 15))]
      (let [q2-start (adj/start-of-quarter june-15)]
        (is (= 4 (:month (day-time-context q2-start))))
        (is (= 1 (:day-in-month (day-time-context q2-start)))))
      (let [q2-end (adj/end-of-quarter june-15)]
        (is (= 6 (:month (day-time-context q2-end))))
        (is (= 30 (:day-in-month (day-time-context q2-end))))))

    ;; Q3 test (September)
    (let [sep-15 (time->value (date 2024 9 15))]
      (let [q3-start (adj/start-of-quarter sep-15)]
        (is (= 7 (:month (day-time-context q3-start))))
        (is (= 1 (:day-in-month (day-time-context q3-start)))))
      (let [q3-end (adj/end-of-quarter sep-15)]
        (is (= 9 (:month (day-time-context q3-end))))
        (is (= 30 (:day-in-month (day-time-context q3-end))))))

    ;; Q4 test (December)
    (let [dec-15 (time->value (date 2024 12 15))]
      (let [q4-start (adj/start-of-quarter dec-15)]
        (is (= 10 (:month (day-time-context q4-start))))
        (is (= 1 (:day-in-month (day-time-context q4-start)))))
      (let [q4-end (adj/end-of-quarter dec-15)]
        (is (= 12 (:month (day-time-context q4-end))))
        (is (= 31 (:day-in-month (day-time-context q4-end))))))))

(deftest test-utility-functions
  (testing "utility and sequence functions"
    (let [start-date (time->value (date 2024 3 10))] ; Sunday March 10, 2024
      ;; Test every-nth-day-of-week - every 2nd Monday
      (let [mondays-seq (adj/every-nth-day-of-week start-date 1 2)
            first-three (take 3 mondays-seq)]
        ;; First should be Monday March 11
        (is (= 1 (day? (first first-three))))
        (is (= 11 (:day-in-month (day-time-context (first first-three)))))
        ;; Second should be Monday March 25 (2 weeks later)
        (is (= 1 (day? (nth first-three 1))))
        (is (= 25 (:day-in-month (day-time-context (nth first-three 1)))))
        ;; Third should be Monday April 8 (2 weeks after that)
        (is (= 1 (day? (nth first-three 2))))
        (is (= 8 (:day-in-month (day-time-context (nth first-three 2)))))
        (is (= 4 (:month (day-time-context (nth first-three 2))))))

      ;; Test business-days-in-range
      (let [friday (time->value (date 2024 3 15)) ; Friday
            next-friday (time->value (date 2024 3 22)) ; Next Friday
            bdays (adj/business-days-in-range friday next-friday)]
        ;; Should include: Fri 15, Mon 18, Tue 19, Wed 20, Thu 21, Fri 22
        (is (= 6 (count bdays)))
        ;; First should be Friday the 15th
        (is (= 5 (day? (first bdays))))
        (is (= 15 (:day-in-month (day-time-context (first bdays)))))
        ;; Last should be Friday the 22nd
        (is (= 5 (day? (last bdays))))
        (is (= 22 (:day-in-month (day-time-context (last bdays)))))))))


(comment
  (def today (date))
  (def bi-weekly-meetings
    (->> (adj/every-nth-day-of-week today 2 2)  ; Every 2nd Tuesday
         (take-while #(< % (add-months today 6)))
         (take 12)
         (map value->time))))
