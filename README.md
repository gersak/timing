
# Timing ⏰

[![Clojars Project](https://img.shields.io/clojars/v/dev.gersak/timing.svg)](https://clojars.org/dev.gersak/timing)

**A time library that thinks in numbers and embraces functional programming.**

Timing offers a different approach to time computation by working in the numeric domain first. 
If you enjoy functional programming, sequences, and immutable data, you might find Timing's 
approach refreshing.

<p align="center">
  <img width="460" height="300" src="resources/images/infinityclock.jpg" style="border-radius:20px;">
</p>

# [API](https://cljdoc.org/d/dev.gersak/timing.core/0.8.1/doc/readme)


## 🤔 Why Try Timing?

### **Zero Dependencies, Simple Design**
- Pure Clojure/ClojureScript with no external dependencies
- Cross-platform compatibility between JVM and JavaScript
- Immutable operations that play nicely with functional code
- Support for multiple calendar systems (Gregorian, Julian, Hebrew, Islamic)
- Holiday awareness for ~200 countries

### **Numbers-First Philosophy**
Instead of working with date objects, Timing encourages you to:
```clojure
;; Work with time as numbers (milliseconds since epoch)
(def now (time->value (date 2024 6 15 14 30 0)))  ; => 1718461800000
(def later (+ now (days 7) (hours 3)))            ; Simple arithmetic!
(value->time later)                               ; Back to Date when needed
; => #inst "2024-06-22T17:30:00.000-00:00"
```

### **Sequence-Friendly Design**
Timing was built to work well with Clojure's sequence operations:
```clojure
;; Generate quarterly dates for 2024
(->> (range 0 12 3)
     (map #(add-months (time->value (date 2024 1 1)) %))
     (map value->time))
; => (#inst "2024-01-01T00:00:00.000-00:00"
;     #inst "2024-04-01T00:00:00.000-00:00" 
;     #inst "2024-07-01T00:00:00.000-00:00"
;     #inst "2024-10-01T00:00:00.000-00:00")

;; Business days using familiar sequence functions
(def q1 (time->value (date 2024 1 15)))
(->> (business-days-in-range (start-of-quarter q1) (end-of-quarter q1))
     (take 5)
     (map value->time))
; => (#inst "2024-01-01T23:00:00.000-00:00"
;     #inst "2024-01-02T23:00:00.000-00:00"
;     #inst "2024-01-03T23:00:00.000-00:00"
;     #inst "2024-01-04T23:00:00.000-00:00"
;     #inst "2024-01-07T23:00:00.000-00:00")
```

### **Flexible Period Arithmetic**
Handle edge cases naturally with smart period functions:
```clojure
;; Fixed-length periods (traditional)
(+ today (days 30) (hours 8))

;; Variable-length periods (handles month/year complexities)
(-> today
    (add-months 3)    ; Handles month lengths properly
    (add-years 2)     ; Handles leap years automatically  
    (+ (days 15)))    ; Mix with fixed periods seamlessly
```

## ⚡ Quick Start

### Installation
```clojure
;; deps.edn - Full umbrella library
{:deps {dev.gersak/timing {:mvn/version "0.8.1"}}}

;; Leiningen
[dev.gersak/timing "0.8.1"]
```

### Modular Installation
You can also pick specific modules:
```clojure
;; Core + standard timezones (current rules only)
{:deps {dev.gersak/timing.core {:mvn/version "0.8.1"}}}

;; Holidays
{:deps {dev.gersak/timing.holidays {:mvn/version "0.8.1"}}}

;; Cron expressions
{:deps {dev.gersak/timing.cron {:mvn/version "0.8.1"}}}
```

> **⚠️ Using Historical Timezones:** `timing.core` depends on `timing.timezones` by default.
> If you need historical timezone data, you must exclude it and use `timing.timezones.full` instead:
> ```clojure
> {:deps {dev.gersak/timing.core {:mvn/version "0.8.1"
>                                 :exclusions [dev.gersak/timing.timezones]}
>         dev.gersak/timing.timezones.full {:mvn/version "0.8.1"}}}
> ```
> Both artifacts provide the same `timing.timezones.db` namespace. The `.full` variant adds ~529KB
> but includes complete IANA tzdata history for accurate historical date calculations.

### Basic Usage
```clojure
(require '[timing.core :as t])

;; Create dates
(def birthday (t/date 1990 5 15))
(def now (t/date))

;; Convert to numeric domain for computation
(def age-ms (- (t/time->value now) (t/time->value birthday)))
(def age-days (/ age-ms t/day))

;; Time arithmetic  
(def next-week (+ (t/time->value now) (t/days 7)))
(def next-month (t/add-months (t/time->value now) 1))

;; Convert back to dates
(t/value->time next-week)
; => #inst "2025-06-08T13:56:08.098-00:00"
(t/value->time next-month)
; => #inst "2025-07-01T13:56:08.098-00:00"
```

## 🎯 Core Features

### **1. Precision Time Units**
```clojure
;; All time units as precise numbers
t/millisecond  ; => 1
t/second      ; => 1000
t/minute      ; => 60000
t/hour        ; => 3600000
t/day         ; => 86400000
t/week        ; => 604800000

;; Helper functions
(t/days 7)      ; => 604800000
(t/hours 3)     ; => 10800000  
(t/minutes 45)  ; => 2700000
```

### **2. Smart Period Arithmetic**
```clojure
;; Variable-length periods with edge case handling
(t/add-months (t/time->value (t/date 2024 1 31)) 1)  
; => Converts Jan 31 -> Feb 28, 2024 (handles month-end properly)

(t/add-months (t/time->value (t/date 2023 1 31)) 1)  
; => Converts Jan 31 -> Feb 27, 2023 (non-leap year handling)

(t/add-years (t/time->value (t/date 2024 2 29)) 1)   
; => Feb 29 -> Feb 27, 2025 (Feb 29 doesn't exist in 2025)

;; Chain operations naturally
(-> (t/time->value (t/date 2024 1 15))
    (t/add-months 6)
    (t/add-years 2)  
    (+ (t/days 10))
    (+ (t/hours 8))
    t/value->time)
; => #inst "2026-07-25T06:00:00.000-00:00"
```

### **3. Flexible Rounding & Alignment**
```clojure
;; Round to any precision
(t/round-number 182.8137 0.25 :up)      ; => 183.0
(t/round-number 182.8137 0.25 :down)    ; => 182.75
(t/round-number 182.8137 0.25 :ceil)    ; => 183.0
(t/round-number 182.8137 0.25 :floor)   ; => 182.75

;; Align to time boundaries
(def test-value (t/time->value (t/date 2024 6 15 14 30 45)))
(t/value->time (t/midnight test-value))           ; Round to start of day
; => #inst "2024-06-14T22:00:00.000-00:00"
(t/value->time (t/round-number test-value t/hour :floor))  ; Round to start of hour
; => #inst "2024-06-15T12:00:00.000-00:00"
```

### **4. Rich Time Context**
```clojure
(t/day-time-context (t/time->value (t/date 2024 6 15)))
; => {:leap-year? true,
;     :day 6,
;     :hour 0,
;     :week 24,
;     :weekend? true,
;     :days-in-month 30,
;     :first-day-in-month? false,
;     :second 0,
;     :days-in-year 366,
;     :value 1718409600000,
;     :month 6,
;     :year 2024,
;     :millisecond 0,
;     :holiday? false,
;     :last-day-in-month? false,
;     :day-in-month 15,
;     :minute 0}
```

### **5. Calendar Frame Generation**
```clojure
;; Get all days in a month (helpful for UI calendars)
(take 3 (t/calendar-frame (t/time->value (t/date 2024 6 1)) :month))
; => ({:day 6, :week 22, :first-day-in-month? true, :value 1717200000000, 
;      :month 6, :year 2024, :last-day-in-month? false, :weekend true, :day-in-month 1}
;     {:day 7, :week 22, :first-day-in-month? false, :value 1717286400000,
;      :month 6, :year 2024, :last-day-in-month? false, :weekend true, :day-in-month 2}
;     {:day 1, :week 23, :first-day-in-month? false, :value 1717372800000,
;      :month 6, :year 2024, :last-day-in-month? false, :weekend false, :day-in-month 3})

;; Also available: :year and :week views
```

## 🛠️ Advanced Features

### **Temporal Adjusters**
```clojure
(require '[timing.adjusters :as adj])

;; Navigate to specific days
(def today (t/time->value (t/date 2024 6 15)))  ; Saturday

(adj/next-day-of-week today 1)        ; Next Monday
; => 1718496000000 (converts to #inst "2024-06-16T22:00:00.000-00:00")

(adj/first-day-of-month-on-day-of-week today 5)  ; First Friday of month
; => 1717716000000 (converts to #inst "2024-06-06T22:00:00.000-00:00")

(adj/last-day-of-month-on-day-of-week today 5)   ; Last Friday of month
; => 1719525600000 (converts to #inst "2024-06-27T22:00:00.000-00:00")

(adj/nth-day-of-month-on-day-of-week today 2 3)  ; 3rd Tuesday of month
; => 1718582400000 (converts to #inst "2024-06-17T22:00:00.000-00:00")

;; Period boundaries
(adj/start-of-week today)             ; Start of current week
(adj/end-of-month today)              ; End of current month  
(adj/start-of-quarter today)          ; Start of current quarter
(adj/end-of-year today)               ; End of current year

;; Business day operations
(adj/next-business-day today)         ; Skip weekends
(adj/add-business-days today 5)       ; Add 5 business days
; => 1718928000000 (converts to #inst "2024-06-20T22:00:00.000-00:00")

(take 3 (map t/value->time (adj/business-days-in-range 
                           (adj/start-of-month today) 
                           (adj/end-of-month today))))
; => (#inst "2024-06-02T22:00:00.000-00:00"
;     #inst "2024-06-03T22:00:00.000-00:00"
;     #inst "2024-06-04T22:00:00.000-00:00")
```

### **Calendar Printing**
```clojure
(require '[timing.util :as util])

(util/print-calendar 2024 6)
; Prints:
;                 June 2024
; +---+---+---+---+---+---+---+
; |Mon|Tue|Wed|Thu|Fri|Sat|Sun|
; +---+---+---+---+---+---+---+
; |   |   |   |   |   | 1 | 2 |
; | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
; |10 |11 |12 |13 |14 |15 |16 |
; |17 |18 |19 |20 |21 |22 |23 |
; |24 |25 |26 |27 |28 |29 |30 |
; +---+---+---+---+---+---+---+

;; Customizable options available
(util/print-calendar 2024 6 {:first-day-of-week 7    ; Sunday first
                              :show-week-numbers true ; Show week numbers
                              :day-width 4})          ; Wider cells

;; Print entire year
(util/print-year-calendar 2024)
```

### **Timezone & Configuration**
```clojure
(require '[timing.timezones.db :as tz])

;; Dynamic timezone context
(t/with-time-configuration {:timezone "America/New_York"}
  (select-keys (t/day-time-context (t/time->value (t/date 2024 6 15)))
               [:year :month :day-in-month :hour]))
; => {:year 2024, :month 6, :day-in-month 15, :hour 0}

;; Work in UTC (ignores local timezone)
(t/with-utc
  (t/day-time-context (t/time->value (t/date 2024 6 15))))

;; Convert between timezones
(def my-time (t/time->value (t/date 2024 6 15 12 0 0)))
(def london-time (t/teleport my-time "Europe/London"))
(t/value->time london-time)
; => #inst "2024-06-15T09:00:00.000-00:00" (adjusted for timezone)

;; Fuzzy timezone matching (helpful suggestions on typos)
(tz/get-timezone "NewYork")  ; Throws with suggestion: "America/New_York"

;; Custom weekend days and holidays
(t/with-time-configuration {:weekend-days #{5 6}      ; Fri/Sat weekend
                            :holiday? my-holiday-fn}   ; Custom holiday logic
  (t/weekend? (t/time->value (t/date 2024 6 14))))    ; Friday
; => true
```

### **Historical Timezone Support**
With `timing.timezones.full`, you can look up timezone rules as they existed at any point in history:
```clojure
(require '[timing.timezones.db :as tz])

;; 2-arity get-timezone returns historical rules
(def ts-1943 (t/time->value (t/utc-date 1943 6 15)))
(def ts-2023 (t/time->value (t/utc-date 2023 6 15)))

(tz/get-timezone "Europe/Belgrade" ts-1943)  ; WWII era rules (C-Eur)
(tz/get-timezone "Europe/Belgrade" ts-2023)  ; Modern EU rules

;; 1-arity returns current rules only
(tz/get-timezone "Europe/Belgrade")  ; Always returns current rules

;; Check if historical data is available
(contains? (get-in tz/db [:zones "Europe/Belgrade"]) :history)
; => true (with timing.timezones.full)
; => false (with timing.timezones)
```

### **Multiple Calendar Systems**
```clojure
;; Switch calendar systems dynamically
(let [now (t/time->value (t/date 2024 6 15))]
  (println "Gregorian:" 
           (select-keys (t/day-time-context now) [:year :month :day-in-month]))
  (println "Hebrew:" 
           (t/with-time-configuration {:calendar :hebrew}
             (select-keys (t/day-time-context now) [:year :month :day-in-month])))
  (println "Islamic:" 
           (t/with-time-configuration {:calendar :islamic}
             (select-keys (t/day-time-context now) [:year :month :day-in-month]))))

; Prints:
; Gregorian: {:year 2024, :month 6, :day-in-month 15}
; Hebrew: {:year 5784, :month 3, :day-in-month 9}
; Islamic: {:year 1445, :month 12, :day-in-month 8}

;; Available calendars: :gregorian, :julian, :hebrew, :islamic
```

### **Holiday Integration**
```clojure
(require '[timing.holiday :as holiday])
;; Note: For full holiday support, also require:
;; (require '[timing.holiday.all]) ; Add all holiday implementations

;; Check holidays by country
(holiday/? :us (t/time->value (t/date 2024 7 4)))     ; => holiday map
(holiday/? :us (t/time->value (t/date 2024 12 25)))   ; => holiday map
(holiday/? :us (t/time->value (t/date 2024 1 1)))     ; => holiday map

;; Get holiday name (important: use holiday/name function!)
(def july4-holiday (holiday/? :us (t/time->value (t/date 2024 7 4))))
(holiday/name :en july4-holiday)  ; => "Independence Day"

(def christmas-holiday (holiday/? :us (t/time->value (t/date 2024 12 25))))
(holiday/name :en christmas-holiday)  ; => "Christmas Day"

;; Supports ~200 countries
```

### **Cron Expression Parser**
```clojure
(require '[timing.cron :as cron])

;; Parse and work with cron expressions
(def next-noon (cron/next-timestamp (t/time->value (t/date 2024 6 15)) "0 0 12 * * ?"))
(t/value->time next-noon)
; => #inst "2024-06-15T10:00:00.000-00:00" (next occurrence of daily noon)

(cron/valid-timestamp? (t/time->value (t/date 2024 6 15 12 0 0)) "0 0 12 * * ?")
; => true (matches cron pattern)

;; Generate future execution times
(def start-time (t/time->value (t/date 2024 6 15)))
(take 3 (map t/value->time (cron/future-timestamps start-time "0 0 9 * * MON")))
; => (#inst "2024-06-17T07:00:00.000-00:00"    ; Next Monday 9 AM
;     #inst "2024-06-24T07:00:00.000-00:00"    ; Following Monday
;     #inst "2024-07-01T07:00:00.000-00:00")   ; And the one after

;; Standard 6-field cron format: second minute hour day-of-month month day-of-week
;; Supports: ranges (1-5), lists (1,3,5), steps (*/15), names (MON, JAN), L/W modifiers
```

## 💡 Real-World Examples

### **Business Date Calculations**
```clojure
;; Add 30 business days to today
(def deadline 
  (adj/add-business-days (t/time->value (t/date 2024 6 15)) 30))
(t/value->time deadline)
; => #inst "2024-07-25T22:00:00.000-00:00"

;; Find all month-end Fridays in 2024
(def month-end-fridays
  (->> (range 1 13)
       (map #(t/time->value (t/date 2024 % 1)))
       (map #(adj/last-day-of-month-on-day-of-week % 5))
       (map t/value->time)))
(take 3 month-end-fridays)
; => (#inst "2024-01-25T23:00:00.000-00:00"
;     #inst "2024-02-22T23:00:00.000-00:00"
;     #inst "2024-03-28T23:00:00.000-00:00")

;; Calculate working days between two dates
(def working-days
  (count (adj/business-days-in-range 
          (t/time->value (t/date 2024 6 1))
          (t/time->value (t/date 2024 6 30)))))
; => 20 (working days in June 2024)
```

### **Recurring Event Generation**
```clojure
;; Every 2nd Tuesday for next 6 months
(def bi-weekly-meetings
  (->> (adj/every-nth-day-of-week today 2 2)  ; Every 2nd Tuesday
       (take-while #(< % (t/add-months today 6)))
       (take 12)
       (map t/value->time)))

;; Quarterly board meetings (last Friday of quarter)
(def quarterly-meetings
  (->> [3 6 9 12]  ; End of quarters
       (map #(t/time->value (t/date 2024 % 1)))
       (map adj/end-of-month)
       (map #(adj/last-day-of-month-on-day-of-week % 5))
       (map t/value->time)))
```

### **Financial Calculations**
```clojure
;; Monthly payment dates (15th of each month)
(def payment-dates-2024
  (->> (range 1 13)
       (map #(t/time->value (t/date 2024 % 15)))
       (map #(if (adj/weekend? %) 
               (adj/previous-business-day %)  ; Move to Friday if weekend
               %))
       (map t/value->time)))

;; Quarter-end reporting dates
(def quarter-ends
  (->> (range 2024 2027)
       (mapcat #(map (fn [q] (adj/end-of-quarter 
                             (t/time->value (t/date % (* q 3) 1)))) 
                     [1 2 3 4]))
       (map t/value->time)))
```

## 🔧 Architecture

### **Modular Design**
```
timing/
├── core/        # Core time computation (timing.core) - no dependencies
├── timezones/   # IANA timezone database (timing.timezones.db)
│   ├── timing.timezones       # Current rules only (~66KB)
│   └── timing.timezones.full  # With historical data (~529KB)
├── holidays/    # Country-specific holidays (timing.holiday)
├── cron/        # Cron expression parser (timing.cron)
└── util/        # Utility functions (timing.util, timing.adjusters)
```

Both timezone artifacts provide the same `timing.timezones.db` namespace. Choose based on your needs:
- **timing.timezones**: Smaller footprint, current timezone rules only
- **timing.timezones.full**: Complete IANA tzdata history for accurate historical date calculations

### **Design Philosophy**

1. **Numeric Domain First** - Computation in milliseconds, objects for display
2. **Immutable Values** - All operations return new values
3. **Functional Composition** - Everything chains naturally with threading macros
4. **Zero Dependencies** - Pure Clojure/ClojureScript
5. **Cross-Platform** - Identical behavior on JVM and JavaScript

### **Performance Characteristics**
- **Efficient** - Numeric arithmetic on primitive longs
- **Memory Friendly** - Minimal object allocation during computation
- **Lazy-Friendly** - Works well with lazy sequences
- **Composable** - Easy to combine with other functional operations

## 🤝 When to Choose Timing

Timing might be a good fit if you:
- Enjoy functional programming patterns
- Prefer working with sequences and transformations
- Want to avoid external dependencies
- Like the numeric domain approach to time
- Need cross-platform Clojure/ClojureScript compatibility
- Appreciate immutable, composable operations

Other excellent time libraries like `clj-time` and Java 8 Time API excel in different areas:
- **clj-time**: Rich object model, extensive parsing/formatting
- **Java 8 Time API**: Comprehensive feature set, strong typing
- **js-joda**: JavaScript port of JSR-310 with excellent browser support

Each approach has its strengths, and the best choice depends on your specific needs and preferences.

## 🎨 Usage Patterns

### **Functional Pipeline Style**
```clojure
(def employees [{:hire-date (t/date 2023 1 15)}
                {:hire-date (t/date 2023 3 20)}
                {:hire-date (t/date 2023 6 10)}])

(->> employees
     (map :hire-date)
     (map t/time->value)  
     (map #(t/add-years % 1))         ; One year anniversary
     (map #(adj/next-day-of-week % 5)) ; Move to Friday
     (map t/value->time)              ; Back to dates
     (take 3))
; => (#inst "2024-01-18T23:00:00.000-00:00"
;     #inst "2024-03-21T23:00:00.000-00:00"
;     #inst "2024-06-13T22:00:00.000-00:00")
```

### **Threading Macro Style**  
```clojure
(-> (t/date 2024 1 1)
    t/time->value
    (t/add-months 6)
    (adj/start-of-quarter)
    (adj/next-business-day)
    t/value->time)
; => #inst "2024-07-01T22:00:00.000-00:00"
```

### **Sequence Generation**
```clojure
;; Generate all Mondays in 2024
(take-while #(< % (t/time->value (t/date 2025 1 1)))
            (adj/every-nth-day-of-week (t/time->value (t/date 2024 1 1)) 1 1))

;; All business days in a month
(def today (t/time->value (t/date 2024 6 15)))
(adj/business-days-in-range (adj/start-of-month today) (adj/end-of-month today))
```

## ⚡ Tips for Best Results

1. **Stay in Numeric Domain** - Minimize conversions to/from Date objects
2. **Embrace Lazy Sequences** - Let Clojure's laziness work for you
3. **Batch Operations** - Process collections functionally
4. **Cache Computations** - Store frequently used values

```clojure
;; Efficient: Stay numeric  
(map #(+ % (t/days 1)) timestamps)

;; Less efficient: Convert back and forth
(map #(t/value->time (+ (t/time->value %) (t/days 1))) dates)
```

## 🚀 Getting Started

1. **Add Timing to your project**
2. **Start with basic date arithmetic**
3. **Explore calendar frames for UI components**
4. **Add temporal adjusters for complex logic**
5. **Use holidays and timezones as needed**

```clojure
;; Your first Timing program
(require '[timing.core :as t])
(require '[timing.adjusters :as adj])

(def today (t/time->value (t/date)))
(def next-friday
  (-> today
      (t/midnight)
      (adj/next-day-of-week 5)
      (+ (t/hours 17))))  ; 5 PM

(println "Next Friday at 5 PM:" (t/value->time next-friday))
```

## 🔧 Important Notes

### **Timezone-Aware Date Display**
Due to timezone handling, dates may display with timezone offsets. This is normal and expected behavior:
```clojure
(t/value->time (t/time->value (t/date 2024 6 15)))
; => #inst "2024-06-14T22:00:00.000-00:00" (with timezone offset)
```

### **Holiday Name Extraction**
Holiday functions return holiday objects that need to be processed with `holiday/name`:
```clojure
;; Don't expect direct string results
(holiday/? :us (t/time->value (t/date 2024 7 4)))
; => {:name #function, ...}

;; Use holiday/name to get readable names
(def holiday-obj (holiday/? :us (t/time->value (t/date 2024 7 4))))
(holiday/name :en holiday-obj)  ; => "Independence Day"
```

### **Rounding Behavior**
The `round-number` function behavior varies by strategy:
```clojure
(t/round-number 182.8137 0.25 :up)    ; => 183.0
(t/round-number 182.8137 0.25 :down)  ; => 182.75
(t/round-number 182.8137 0.25 :ceil)  ; => 183.0
(t/round-number 182.8137 0.25 :floor) ; => 182.75
```

## 📜 License

Copyright © 2018 Robert Gersak

Released under the MIT license.

---

*Timing: A friendly approach to time computation in Clojure.*
