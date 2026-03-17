#!/usr/bin/env bb

(require '[timing.core :as t])
(require '[timing.timezones.db :as tz])
(require '[timing.holiday :as holiday])
(require '[timing.holiday.pl])  ;; Load Polish holidays
(require '[timing.holiday.us])  ;; Load US holidays

(println "=== Testing Full Timezones ===")

;; Check if history is present
(println "\nHistory present:"
         (contains? (get-in tz/db [:zones "Europe/Belgrade"]) :history))

;; Test historical timezone lookup
(println "\nHistorical Europe/Belgrade:")
(let [ts-1943 (t/time->value (t/utc-date 1943 6 15))
      ts-2023 (t/time->value (t/utc-date 2023 6 15))
      zone-1943 (tz/get-timezone "Europe/Belgrade" ts-1943)
      zone-2023 (tz/get-timezone "Europe/Belgrade" ts-2023)]
  (println "  1943 (WWII):" (:rule zone-1943) "(expected: C-Eur)")
  (println "  2023 (now):" (:rule zone-2023) "(expected: EU)"))

;; Test fuzzy timezone matching
(println "\nFuzzy timezone matching:")
(try
  (tz/get-timezone "NewYork")
  (catch Exception e
    (println "  " (ex-message e))))

(println "\n=== Testing Holidays ===")

;; Test Polish holidays - check specific dates
(println "\nPolish holidays check:")
(let [dates [(t/date 2025 1 1)    ;; New Year
             (t/date 2025 2 27)   ;; Fat Thursday 2025 (easter -46)
             (t/date 2025 4 20)   ;; Easter 2025
             (t/date 2025 11 11)  ;; Independence Day
             (t/date 2025 12 25)]] ;; Christmas
  (doseq [d dates]
    (let [result (holiday/? :pl d)]
      (println "  " d "->" (if result
                             (or (holiday/name :pl result)
                                 (holiday/name :en result)
                                 result)
                             "not a holiday")))))

;; Test US holidays
(println "\nUS holidays check:")
(let [dates [(t/date 2025 1 1)    ;; New Year
             (t/date 2025 7 4)    ;; Independence Day
             (t/date 2025 12 25)]] ;; Christmas
  (doseq [d dates]
    (let [result (holiday/? :us d)]
      (println "  " d "->" (if result
                             (or (holiday/name :en result) result)
                             "not a holiday")))))

(println "\n=== All tests completed ===")
