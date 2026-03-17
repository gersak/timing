# Cron Expression Syntax

This document describes the cron expression syntax supported by `timing.cron`.

## Format

```
second minute hour day-of-month month day-of-week [year]
```

### Short Forms

Expressions with fewer than 6 fields are automatically padded:

| Input | Expanded | Description |
|-------|----------|-------------|
| `*/10` | `*/10 * * * * *` | Every 10 seconds |
| `0 */5` | `0 */5 * * * *` | Every 5 minutes |
| `0 0 9` | `0 0 9 * * *` | Daily at 9:00 |
| `0 0 9 * *` | `0 0 9 * * *` | Daily at 9:00 |
| `* * * * MON-FRI` | `0 * * * * MON-FRI` | 5-field Unix cron (prepends `0` for seconds) |

### @ Shortcuts

| Shortcut | Equivalent | Description |
|----------|------------|-------------|
| `@yearly` | `0 0 0 1 1 *` | Once a year (Jan 1st) |
| `@annually` | `0 0 0 1 1 *` | Same as @yearly |
| `@monthly` | `0 0 0 1 * *` | First of every month |
| `@weekly` | `0 0 0 * * 1` | Every Monday |
| `@daily` | `0 0 0 * * *` | Every day at midnight |
| `@midnight` | `0 0 0 * * *` | Same as @daily |
| `@hourly` | `0 0 * * * *` | Every hour |

| Field | Required | Values | Special Characters |
|-------|----------|--------|-------------------|
| Second | Yes | 0-59 | `*` `,` `-` `/` |
| Minute | Yes | 0-59 | `*` `,` `-` `/` |
| Hour | Yes | 0-23 | `*` `,` `-` `/` |
| Day of Month | Yes | 1-31 | `*` `,` `-` `/` `?` `L` `W` |
| Month | Yes | 1-12 or JAN-DEC | `*` `,` `-` `/` |
| Day of Week | Yes | 1-7 or MON-SUN | `*` `,` `-` `/` `?` `L` `#` |
| Year | No | any | `*` `,` `-` `/` |

## Basic Expressions

### Wildcards

| Expression | Meaning |
|------------|---------|
| `*` | Any value |
| `?` | Any value (alias for `*`) |

### Fixed Values

```clojure
"0 0 12 * * *"      ; Every day at 12:00:00
"0 30 9 * * *"      ; Every day at 09:30:00
"0 0 0 1 * *"       ; First day of every month at midnight
```

### Ranges

Use `-` to specify a range of values.

```clojure
"0 0 9-17 * * *"    ; Every hour from 9:00 to 17:00
"0 0 0 1-15 * *"    ; Days 1-15 of every month
"0 0 0 * * MON-FRI" ; Monday through Friday
```

### Lists

Use `,` to specify multiple values.

```clojure
"0 0 9,12,18 * * *"   ; At 9:00, 12:00, and 18:00
"0 0 0 1,15 * *"      ; 1st and 15th of every month
"0 0 0 * * MON,WED,FRI" ; Monday, Wednesday, Friday
```

### Steps/Intervals

Use `/` to specify intervals.

```clojure
"*/10 * * * * *"    ; Every 10 seconds
"0 */15 * * * *"    ; Every 15 minutes
"0 0 */2 * * *"     ; Every 2 hours
"0 0 0 */5 * *"     ; Every 5 days
```

## Combining Expressions

### Multiple Ranges

Combine multiple ranges with commas.

```clojure
"0 0 9-12,14-18 * * *"  ; 9:00-12:00 and 14:00-18:00
"0 0 0 1-5,15-20 * *"   ; Days 1-5 and 15-20
"0 0 0 * 1-3,7-9 *"     ; Q1 and Q3 (Jan-Mar, Jul-Sep)
```

### Ranges with Steps

Apply steps to ranges.

```clojure
"0 0-30/10 * * * *"   ; Minutes 0, 10, 20, 30
"0 0 8-18/2 * * *"    ; 8:00, 10:00, 12:00, 14:00, 16:00, 18:00
"0 0 0 1-15/3 * *"    ; Days 1, 4, 7, 10, 13
```

### Mixed Lists, Ranges, and Steps

```clojure
"0 0,15,30,45 * * * *"     ; Every 15 minutes (explicit)
"0 0 9,12-14,18 * * *"     ; 9:00, 12:00-14:00, 18:00
"0 0 0 1,10-15,L * *"      ; 1st, 10th-15th, and last day
```

## Named Values

### Month Names

Both abbreviated and full names are supported (case-insensitive).

| Abbreviation | Full Name | Value |
|--------------|-----------|-------|
| JAN | JANUARY | 1 |
| FEB | FEBRUARY | 2 |
| MAR | MARCH | 3 |
| APR | APRIL | 4 |
| MAY | MAY | 5 |
| JUN | JUNE | 6 |
| JUL | JULY | 7 |
| AUG | AUGUST | 8 |
| SEP | SEPTEMBER | 9 |
| OCT | OCTOBER | 10 |
| NOV | NOVEMBER | 11 |
| DEC | DECEMBER | 12 |

```clojure
"0 0 0 1 JAN *"           ; January 1st
"0 0 0 * JAN-MAR *"       ; Q1 (January through March)
"0 0 0 * JAN,APR,JUL,OCT *" ; First month of each quarter
```

### Day Names

Both abbreviated and full names are supported (case-insensitive).

| Abbreviation | Full Name | Value |
|--------------|-----------|-------|
| MON | MONDAY | 1 |
| TUE | TUESDAY | 2 |
| WED | WEDNESDAY | 3 |
| THU | THURSDAY | 4 |
| FRI | FRIDAY | 5 |
| SAT | SATURDAY | 6 |
| SUN | SUNDAY | 7 |

```clojure
"0 0 9 * * MON"           ; Every Monday at 9:00
"0 0 0 * * MON-FRI"       ; Weekdays
"0 0 0 * * SAT,SUN"       ; Weekends
```

## Special Expressions

### L - Last

#### Last Day of Month (day-of-month field)

```clojure
"0 0 0 L * *"             ; Last day of every month
```

#### Days Before Last (day-of-month field)

```clojure
"0 0 0 L-1 * *"           ; Second to last day
"0 0 0 L-3 * *"           ; 3 days before last day
```

#### Last Weekday Occurrence (day-of-week field)

```clojure
"0 0 0 * * FRIL"          ; Last Friday of every month
"0 0 0 * * 1L"            ; Last Monday of every month
"0 0 0 * * SUNL"          ; Last Sunday of every month
```

### W - Nearest Weekday

Finds the nearest weekday (Mon-Fri) to the specified day. Does not cross month boundaries.

```clojure
"0 0 0 15W * *"           ; Nearest weekday to the 15th
"0 0 0 1W * *"            ; Nearest weekday to the 1st
```

**Behavior:**
- If the day is Saturday, fires on Friday (unless Friday is in previous month, then Monday)
- If the day is Sunday, fires on Monday (unless Monday is in next month, then Friday)
- If the day is already a weekday, fires on that day

### LW - Last Weekday of Month

```clojure
"0 0 0 LW * *"            ; Last weekday (Mon-Fri) of every month
```

### # - Nth Occurrence

Specifies the Nth occurrence of a weekday in the month.

```clojure
"0 0 0 * * FRI#1"         ; First Friday of every month
"0 0 0 * * FRI#3"         ; Third Friday of every month
"0 0 0 * * MON#2"         ; Second Monday of every month
"0 0 0 * * 5#1"           ; First Friday (using number)
```

**Note:** The occurrence value must be between 1 and 5. If there's no Nth occurrence in a month (e.g., 5th Friday), that month is skipped.

## Complex Examples

### Business Hours

```clojure
;; Every 15 minutes during business hours on weekdays
"0 */15 9-17 * * MON-FRI"

;; Every hour from 9 AM to 5 PM, Monday through Friday
"0 0 9-17 * * MON-FRI"
```

### Monthly Reports

```clojure
;; First day of each month at 6:00 AM
"0 0 6 1 * *"

;; Last day of each month at midnight
"0 0 0 L * *"

;; Last weekday of each month at 5:00 PM
"0 0 17 LW * *"

;; Third Friday of each month (options expiration)
"0 0 16 * * FRI#3"
```

### Quarterly Tasks

```clojure
;; First day of each quarter at midnight
"0 0 0 1 JAN,APR,JUL,OCT *"

;; Last day of each quarter
"0 0 0 L MAR,JUN,SEP,DEC *"
```

### Payroll (Bi-weekly)

```clojure
;; Every other Friday at 6:00 AM
;; Note: Use FRI#1 and FRI#3, or FRI#2 and FRI#4
"0 0 6 * * FRI#1"   ; First Friday
"0 0 6 * * FRI#3"   ; Third Friday
```

### Maintenance Windows

```clojure
;; Every Sunday at 2:00 AM
"0 0 2 * * SUN"

;; First Sunday of each month at 3:00 AM
"0 0 3 * * SUN#1"

;; Last Saturday of each month at 11:00 PM
"0 0 23 * * SATL"
```

### Complex Schedules

```clojure
;; Every 30 minutes from 8 AM to 6 PM on weekdays
"0 0,30 8-18 * * MON-FRI"

;; 9:00 and 17:00 on weekdays, 10:00 on weekends
;; (requires two separate cron expressions)
"0 0 9,17 * * MON-FRI"
"0 0 10 * * SAT,SUN"

;; Every 5 minutes during first half of each hour
"0 0-30/5 * * * *"
```

## API Usage

```clojure
(require '[timing.cron :as cron])
(require '[timing.core :as core])

;; Check if a timestamp matches a cron expression
(cron/valid-timestamp? (core/date 2024 1 31 0 0 0) "0 0 0 L * *")
;; => true

;; Get the next matching timestamp
(cron/next-timestamp (core/date 2024 1 1 0 0 0) "0 0 0 L * *")
;; => 1706659200000

;; Get a lazy sequence of future timestamps
(take 5 (cron/future-timestamps (core/date 2024 1 1 0 0 0) "0 0 0 L * *"))
;; => (1706659200000 1709164800000 1711843200000 ...)

;; Convert timestamp to date
(core/value->date 1706659200000)
;; => #inst "2024-01-31T00:00:00.000Z"
```

## Notes

1. **Day-of-month and Day-of-week**: When both are specified (not `*` or `?`), timestamps must match BOTH constraints.

2. **Month boundaries**: The `W` (nearest weekday) expression does not cross month boundaries. If the 1st is a Saturday, it fires on Monday the 3rd, not Friday of the previous month.

3. **Leap years**: The `L` expression correctly handles February in leap years (29 days).

4. **Performance**: The implementation uses smart iteration - it only generates valid timestamps rather than checking every possible time. This makes expressions like `"0 0 0 L * *"` very efficient.
