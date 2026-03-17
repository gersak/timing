(ns timing.timezones.historical-test
  "Tests for historical timezone resolution.
   These tests verify that get-timezone and get-rule correctly
   return historical data when provided with timestamps.

   Tests are designed to work with both:
   - timing.timezones (current only) - historical lookups return current data
   - timing.timezones.full (with history) - historical lookups return period-specific data"
  (:require [clojure.test :refer :all]
            [timing.core :as t]
            [timing.timezones.db :as tz]))

;; Helper to convert year/month/day to UTC timestamp
(defn ->timestamp [year month day]
  (t/time->value (t/utc-date year month day)))

;; Detect if we have the full historical database loaded
(defn has-history?
  "Returns true if the loaded timezone database has historical data."
  []
  (let [belgrade-raw (get-in tz/db [:zones "Europe/Belgrade"])]
    (and (map? belgrade-raw)
         (contains? belgrade-raw :history))))

;; =============================================================================
;; Zone History Tests
;; =============================================================================

(deftest get-timezone-current-test
  (testing "get-timezone without timestamp returns current data"
    (let [zone (tz/get-timezone "Europe/Belgrade")]
      (is (map? zone))
      (is (contains? zone :offset))
      (is (contains? zone :rule))
      (is (= "EU" (:rule zone)) "Current rule should be EU")
      (is (= 3600000 (:offset zone)) "Current offset should be +1 hour (CET)"))))

(deftest get-timezone-2-arity-current-test
  (testing "get-timezone with nil timestamp returns current data"
    (let [zone-1-arity (tz/get-timezone "Europe/Belgrade")
          zone-2-arity (tz/get-timezone "Europe/Belgrade" nil)]
      (is (= zone-1-arity zone-2-arity)
          "nil timestamp should return same as 1-arity"))))

(deftest get-timezone-historical-test
  (testing "Europe/Belgrade historical zones"
    ;; Europe/Belgrade history (from tzdata):
    ;; - Before 1884: LMT (Local Mean Time) offset ~1:22:00
    ;; - 1884-1941: CET (Central European Time) +1:00
    ;; - 1941-1945: C-Eur rules (Nazi occupation)
    ;; - 1945-1982: Various changes
    ;; - 1982-present: CET/CEST with EU rules

    (if (has-history?)
      (do
        (testing "1900 - should be CET without DST rules"
          (let [zone (tz/get-timezone "Europe/Belgrade" (->timestamp 1900 6 15))]
            (is (= 3600000 (:offset zone)) "1900 offset should be +1 hour")
            (is (= "-" (:rule zone)) "1900 should have no DST rule")))

        (testing "1943 - should be C-Eur rules (WWII Nazi occupation)"
          (let [zone (tz/get-timezone "Europe/Belgrade" (->timestamp 1943 6 15))]
            (is (= 3600000 (:offset zone)) "1943 offset should be +1 hour")
            (is (= "C-Eur" (:rule zone)) "1943 should use C-Eur DST rules")))

        (testing "2023 - should be EU rules"
          (let [zone (tz/get-timezone "Europe/Belgrade" (->timestamp 2023 6 15))]
            (is (= 3600000 (:offset zone)) "2023 offset should be +1 hour")
            (is (= "EU" (:rule zone)) "2023 should use EU DST rules"))))

      ;; Current-only database: all timestamps return current data
      (testing "Current-only database: timestamps ignored"
        (let [zone-1900 (tz/get-timezone "Europe/Belgrade" (->timestamp 1900 6 15))
              zone-current (tz/get-timezone "Europe/Belgrade")]
          (is (= zone-1900 zone-current)
              "Without history, timestamp should be ignored"))))))

(deftest get-timezone-link-resolution-test
  (testing "Timezone links resolve correctly with historical lookup"
    ;; Europe/Zagreb is a link to Europe/Belgrade
    (let [zagreb-current (tz/get-timezone "Europe/Zagreb")
          belgrade-current (tz/get-timezone "Europe/Belgrade")]
      (is (= zagreb-current belgrade-current)
          "Zagreb should resolve to Belgrade"))

    (let [zagreb-1943 (tz/get-timezone "Europe/Zagreb" (->timestamp 1943 6 15))
          belgrade-1943 (tz/get-timezone "Europe/Belgrade" (->timestamp 1943 6 15))]
      (is (= zagreb-1943 belgrade-1943)
          "Zagreb 1943 should resolve to Belgrade 1943"))))

(deftest get-timezone-america-new-york-test
  (testing "America/New_York historical zones"
    ;; New York has had various DST rules over the years

    (testing "Current period uses US rules"
      (let [zone (tz/get-timezone "America/New_York")]
        (is (= -18000000 (:offset zone)) "Current offset should be -5 hours (EST)")
        (is (= "US" (:rule zone)) "Current should use US DST rules")))

    (testing "Historical lookup returns valid zone data"
      (let [zone-1950 (tz/get-timezone "America/New_York" (->timestamp 1950 6 15))]
        (is (map? zone-1950))
        (is (contains? zone-1950 :offset))
        (is (= -18000000 (:offset zone-1950)) "1950 offset should be -5 hours")))))

(deftest get-timezone-no-history-test
  (testing "Timezones without history work correctly"
    ;; Some timezones may not have historical changes
    (let [gmt (tz/get-timezone "GMT")]
      (is (= 0 (:offset gmt)) "GMT offset should be 0")

      ;; With timestamp should also work
      (let [gmt-historical (tz/get-timezone "GMT" (->timestamp 1900 1 1))]
        (is (= 0 (:offset gmt-historical)) "Historical GMT offset should be 0")))))

;; =============================================================================
;; Rule History Tests
;; =============================================================================

(deftest get-rule-current-test
  (testing "get-rule without timestamp returns current data"
    (let [rule (tz/get-rule "EU")]
      (is (map? rule))
      (is (contains? rule :daylight-savings))
      (is (contains? rule :standard))
      (is (= 3 (get-in rule [:daylight-savings :month])) "EU DST starts in March")
      (is (= 10 (get-in rule [:standard :month])) "EU standard time starts in October"))))

(deftest get-rule-2-arity-current-test
  (testing "get-rule with nil timestamp returns current data"
    (let [rule-1-arity (tz/get-rule "EU")
          rule-2-arity (tz/get-rule "EU" nil)]
      (is (= rule-1-arity rule-2-arity)
          "nil timestamp should return same as 1-arity"))))

(deftest get-rule-nonexistent-test
  (testing "get-rule for nonexistent rule returns nil"
    (is (nil? (tz/get-rule "NONEXISTENT")))
    (is (nil? (tz/get-rule "NONEXISTENT" (->timestamp 2023 1 1))))))

(deftest get-rule-no-dst-test
  (testing "Rule '-' (no DST) returns nil or minimal data"
    ;; The "-" rule means no DST, should not be in rules
    (is (nil? (tz/get-rule "-")))))

;; =============================================================================
;; API Compatibility Tests
;; =============================================================================

(deftest api-compatibility-test
  (testing "2-arity functions work as documented"
    ;; Both get-timezone and get-rule should accept 2 arguments
    (is (map? (tz/get-timezone "Europe/London" (->timestamp 2000 1 1))))
    (is (map? (tz/get-rule "EU" (->timestamp 2000 1 1))))

    ;; Future timestamps should return current data
    (let [future-ts (->timestamp 2100 1 1)]
      (is (= (tz/get-timezone "Europe/London")
             (tz/get-timezone "Europe/London" future-ts))
          "Future timestamp should return current zone data")
      (is (= (tz/get-rule "EU")
             (tz/get-rule "EU" future-ts))
          "Future timestamp should return current rule data"))))

(deftest edge-cases-test
  (testing "Edge case: very old date (before timezone standardization)"
    (let [zone (tz/get-timezone "Europe/London" (->timestamp 1800 1 1))]
      (is (map? zone) "Should return some zone data for 1800")
      (is (contains? zone :offset))))

  (testing "Edge case: epoch (1970-01-01)"
    (let [zone (tz/get-timezone "America/New_York" 0)]
      (is (map? zone) "Should return zone data for epoch")))

  (testing "Edge case: negative timestamp (before 1970)"
    (let [zone (tz/get-timezone "Europe/Paris" -86400000)] ; 1969-12-31
      (is (map? zone) "Should return zone data for 1969"))))

;; =============================================================================
;; History Boundary Tests
;; =============================================================================

(deftest history-boundary-test
  (when (has-history?)
    (testing "Timezone changes at correct boundary"
      ;; Europe/Belgrade changed to EU rules around 1982-10-31
      ;; Before that it was CET without EU rules

      (let [before-eu (tz/get-timezone "Europe/Belgrade" (->timestamp 1982 1 1))
            after-eu (tz/get-timezone "Europe/Belgrade" (->timestamp 1983 1 1))]

        ;; Both should have CET offset
        (is (= 3600000 (:offset before-eu)))
        (is (= 3600000 (:offset after-eu)))

        ;; But different rules
        (is (= "-" (:rule before-eu)) "Before 1982-10 should have no DST rule")
        (is (= "EU" (:rule after-eu)) "After 1982-10 should have EU rule")))))

;; =============================================================================
;; Integration with timing.core Tests
;; =============================================================================

(deftest integration-with-core-test
  (testing "Timezone data works with timing.core"
    ;; This tests that the timezone data structure is compatible
    ;; with what timing.core expects

    (let [zone (tz/get-timezone "Europe/Belgrade" (->timestamp 2023 6 15))]
      ;; Should have required fields for timing.core
      (is (number? (:offset zone)))
      (is (or (string? (:rule zone)) (number? (:rule zone))))
      (is (string? (:format zone)))))

  (when (has-history?)
    (testing "Historical timezone data works with timing.core"
      (let [zone (tz/get-timezone "Europe/Belgrade" (->timestamp 1943 6 15))]
        (is (number? (:offset zone)))
        (is (= "C-Eur" (:rule zone)))
        (is (string? (:format zone)))))))

(deftest available-zones-test
  (testing "available-zones contains expected timezones"
    (is (some #(= % "Europe/London") tz/available-zones))
    (is (some #(= % "America/New_York") tz/available-zones))
    (is (some #(= % "Asia/Tokyo") tz/available-zones))
    (is (some #(= % "Australia/Sydney") tz/available-zones))))

(comment
  ;; Manual testing helpers

  ;; View raw zone data with history
  (get-in tz/db [:zones "Europe/Belgrade"])

  ;; View zone at specific time
  (tz/get-timezone "Europe/Belgrade" (->timestamp 1943 6 15))

  ;; View current zone
  (tz/get-timezone "Europe/Belgrade")

  ;; View EU rule
  (tz/get-rule "EU"))
