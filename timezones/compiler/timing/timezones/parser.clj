(ns timing.timezones.parser
  (:refer-clojure :exclude [second])
  (:require 
    clojure.string
    clojure.java.io
    [timing.core :refer :all]
    [instaparse.core :as insta]
    clojure.pprint))


(def ^:dynamic *tz-dir* nil)
(alter-var-root #'timing.core/*timezone* (constantly nil))

(def zones ["europe" "africa" "northamerica" "pacificnew" "southamerica" "asia" "australasia" "etcetera" "backward"])

(defn zone-definition [zone]
  (slurp (clojure.java.io/file (str *tz-dir* (name zone)))))

(defn locale-definition []
  (slurp (clojure.java.io/file (str *tz-dir* "zone.tab"))))


(def months-mapping
  {"Jan" 1
   "Feb" 2
   "Mar" 3
   "Apr" 4
   "May" 5
   "Jun" 6
   "Jul" 7
   "Aug" 8
   "Sep" 9
   "Oct" 10
   "Nov" 11
   "Dec" 12})

(def zone-grammar
  "number = #'[0-9]+'
   <comment> = #'#.*'
   <word> = #'[a-zA-Z]+'
   <space> = (' ' | '\\t')+
   <newline>= '\\n' | '\\n\\r'
   year = number
   month-name = 'Jan' | 'Feb' | 'Mar' | 'Apr' | 'May' | 'Jun' | 'Jul' | 'Aug' | 'Sep' | 'Oct' | 'Nov' | 'Dec'
   month = month-name
   day = number
   floating-day = #'last\\w+' | #'\\w+>=\\d+|\\w+<=\\d+'
   second = number
   minute = number
   hour = number 
   time-suffix = #'s|g|u|z'
   time = hour <':'> minute (<':'> second)? time-suffix?

   <zone-start> =  'Zone'
   sign = '+' | '-'
   gmtoff = sign? hour <':'> minute (<':'> second)? | sign? hour 
   <zone-name> = #'[a-zA-Z/_\\-0-9+-]+'
   zone-rule = #'[a-zA-Z\\-_]+'  | gmtoff
   zone-format = #'[A-Z\\-/%s\\+0-9a-z]+'
   zone-until = <space> year (<space> month)? (<space> day | <space> floating-day)? (<space> time)?
   zone-offset = gmtoff
   zone-row = <space> zone-offset <space> zone-rule <space> zone-format (zone-until / <space>?) <space>? (<newline> | Epsilon)
   zone-definition = <zone-start> <space> zone-name zone-row+
   
   link-alias = zone-name 
   link-canonical = zone-name
   link-definition = <'Link'> <space>+ link-canonical <space> link-alias <space>? (<newline> | Epsilon)

   rule-name = #'[a-zA-Z\\-_]+'
   rule-type = '-' | word
   rule-from = year
   rule-to = year | 'only' | 'max'
   rule-in = month
   rule-on = day | floating-day 
   rule-at = time
   rule-save = gmtoff | number
   rule-letters = #'[a-zA-Z\\+\\-0-9]+' | '-'
   rule-definition = <'Rule'> <space> rule-name <space> rule-from <space> rule-to <space> 
                     rule-type <space> rule-in <space> rule-on <space> rule-at <space> 
                     rule-save <space> rule-letters <space>? (<newline> | Epsilon)


   <empty-space> = (<space> <newline> | <newline>)+

   timezone = (rule-definition | zone-definition | link-definition)+")

(def zone-parser (insta/parser zone-grammar))


(defn read-zone [zone]
  (insta/transform
    {:number #(Integer/parseInt %)
     :month-name months-mapping
     :time (fn [& args] (vector :time (reduce conj {} args)))
     :gmtoff (fn [& args]
               (let [{:keys [hour minute sign second]
                      :or {sign "+"
                           hour 0
                           minute 0
                           second 0} :as all} (reduce conj {} args)]
                 (* (if (= sign "-") -1 1) 
                    (+ 
                      (hours hour)
                      (minutes minute)
                      (seconds second)))))
     :zone-until (fn [& args] 
                   (let [time (reduce conj {} args)]
                     [:until (if (empty? time) nil time)]))
     :zone-row (fn [& args]
                 (let [[[_ offset]
                        [_ rule]
                        [_ format]
                        [_ until]] args]
                   {:offset offset
                    :rule rule
                    :format format
                    :until until}))
     :rule-definition (fn [[_ rule-name] & args]
                        [:rule (vector rule-name (reduce conj {} args))])
     :link-definition (fn [& args] 
                       [:link (reduce conj {} args)])}
    (->
      (zone-definition zone)
      (clojure.string/replace #"#.*" "")
      clojure.string/split-lines
      (#(remove (comp empty? clojure.string/trim) %))
      (#(clojure.string/join "\n" %))
      (zone-parser :start :timezone))))

(defn extract-zones [parsed-data]
  (let [timezone (rest parsed-data)
        zones (filter (comp #{:zone-definition} first) timezone)
        links (filter (comp #{:link} first) timezone)
        zones' (reduce
                 (fn [r [_ zname & rows]]
                   (assoc r zname rows))
                 {}
                 zones)]
    (reduce
      (fn [r [_ {:keys [link-alias link-canonical]}]]
        (assoc r link-alias link-canonical))
      zones'
      links)))

(defn extract-rules [parsed-data]
  (let [timezone (rest parsed-data)
        rules (map clojure.core/second (filter (comp #{:rule} first) timezone))]
    (reduce
      (fn [r [k v]]
        (assoc r k (mapv clojure.core/second v)))
      {}
      (group-by first rules))))

(defn extract-data [parsed-data]
  {:zones (extract-zones parsed-data)
   :rules (extract-rules parsed-data)})



(defn process-zone [rules]
  (loop [rules rules 
         history []]
    (if (empty? rules)
      (let [current (dissoc (last history) :until)
            history (vec (butlast history))] 
        ;; Return result in form of actual current timezone
        ;; and previous history
        {:current (assoc current :from (:until (last history)))
         :history history})
      (if (empty? history)
        ;; If there is nothing in history, than create one
        (let [[current & rules] rules
              ;; Use current rule
              current (update 
                        current
                        :until
                        ;; And update until with UTC value
                        (fn [{:keys [year month day]
                              :or {day 1 month 1}
                              :as until}]
                          (when until 
                            (date->utc-value (utc-date year month day)))))]
          ;; Recur on rest of rules with new history
          (recur rules (conj history current)))
        ;; There is history so we can compare previous offset
        (let [[current & rules] rules
              {previous-offset :offset
               :or {previous-offset 0}} (last history)]
          (recur
            rules
            (conj
              history
              (case (-> current :until :time :time-suffix)
                  ;; If it is standard clock time than use previous offset to
                  ;; calculate exact UTC time.... TODO - maybe include rules if not to complicated for now
                  "s" (binding [*offset* previous-offset]
                        (update current :until until-value))
                  ;; Otherwise use 0 offset for UTC
                  ("u" "g" "z") (binding [*offset* 0]
                                  (update current :until until-value))
                  (update current :until until-value)))))))))

(defn savings-rule? [{:keys [rule-save]}] 
  (when (and rule-save (number? rule-save)) 
    (not (zero? rule-save))))

(def ^:private MAX_YEAR 10000)
(def ^:private MAX_YEAR_VALUE (-> MAX_YEAR date time->value))

(defn rule-until? [{[_ from :as rule-from] :rule-from 
                    [_ to :as rule-to] :rule-to}]
  (case rule-to
    "only" from
    "max" MAX_YEAR
    to))


(defn rule-interval [{[_ from] :rule-from
                      [_ to :as rule-to] :rule-to}]
  (case rule-to
    "only" [from 
            (inc from)]
    "max" [from
           MAX_YEAR_VALUE]
    [from (inc from)]))


(defn rule-data [{:keys [rule-from rule-on rule-save rule-at rule-in]}]
  (let [timestamp (reduce
                    conj
                    {}
                    [rule-from rule-on rule-at rule-in])]
    [((comp until-value) timestamp) 
     (case (:time-suffix (:time timestamp))
       "s" :standard
       :utc)
     (->
       timestamp
       (assoc :save rule-save)
       (dissoc :year))]))

(defn pair-rule [{[_ rule-from] :rule-from :as rule} rest-rules]
  (let [target (if (savings-rule? rule) 
                 (complement savings-rule?)
                 savings-rule?)]
    (letfn [(pair? [rule]
              (and
                (target rule)
                (>= (rule-until? rule) rule-from)))] 
      (first 
        (filter
          pair?
          rest-rules)))))


(defn process-rules [rules]
  (letfn [(active-rule? [{:keys [rule-to]}]
            (= "max" rule-to))]
   (loop [[rule & rest-rules] (sort-by rule-until? rules)
         history []
         active nil]
     (if (empty? rule) {:current active
                        :history history} 
      (if (active-rule? rule) 
        (recur
          rest-rules
          history
          (let [[start clock rule'] (rule-data rule)]
            (assoc 
              active 
              (if (savings-rule? rule) :daylight-savings :standard)
              (merge 
                {:from start 
                 :clock? clock}
                rule'))))
        (if-let [rule-pair (pair-rule rule rest-rules)]
          (recur
            rest-rules
            (let [[start clock rule] (rule-data rule)
                  [end clock'] (rule-data rule-pair)]
              (conj history
                    [[{:utc start :clock? clock} 
                      {:utc end :clock? clock'}] 
                     rule]))
            active)
          (recur rest-rules history active)))))))


(defn create-timezone-data
  "Creates timezone data with current rules only (no history)."
  []
  (reduce
    (fn [r zone]
      (let [{:keys [zones rules]} (extract-data (read-zone zone))]
        (let [zones' (reduce
                       (fn [result [zone rules]]
                         (assoc result
                           zone
                           (if (string? rules) rules (:current (process-zone rules)))))
                       {}
                       zones)
              rules' (reduce
                       (fn [result [rule-name rules]]
                         (assoc result
                           rule-name
                           (:current (process-rules rules))))
                       {}
                       rules)]
          (->
            r
            (update :zones merge zones')
            (update :rules merge rules')))))
    {}
    [:europe
     :africa
     :northamerica
     :southamerica
     :asia
     :australasia
     :backward
     :etcetera]))

(defn create-timezone-data-full
  "Creates timezone data with full historical rules."
  []
  (reduce
    (fn [r zone]
      (let [{:keys [zones rules]} (extract-data (read-zone zone))]
        (let [zones' (reduce
                       (fn [result [zone rules]]
                         (assoc result
                           zone
                           (if (string? rules)
                             rules
                             (let [{:keys [current history]} (process-zone rules)]
                               (if (seq history)
                                 {:current current :history history}
                                 current)))))
                       {}
                       zones)
              rules' (reduce
                       (fn [result [rule-name rules]]
                         (let [{:keys [current history]} (process-rules rules)]
                           (assoc result
                             rule-name
                             (if (seq history)
                               {:current current :history history}
                               current))))
                       {}
                       rules)]
          (->
            r
            (update :zones merge zones')
            (update :rules merge rules')))))
    {}
    [:europe
     :africa
     :northamerica
     :southamerica
     :asia
     :australasia
     :backward
     :etcetera]))


(defn create-locale-data []
  (let [definition-lines (remove
                           #(clojure.string/starts-with? % "#")
                           (clojure.string/split-lines (locale-definition)))]
    (reduce
      (fn [result line]
        (let [[locale coordinates zone] (clojure.string/split line #"\s+")]
          (assoc result locale {:coordinates coordinates :zone zone})))
      nil
      definition-lines)))

(defn compile-tz-db [zone-target]
  (clojure.pprint/pprint
    (create-timezone-data)
    (clojure.java.io/writer zone-target)))

(comment 
  (clojure.pprint/pprint
    (create-timezone-data)
    (clojure.java.io/writer zone-target))
  (clojure.pprint/pprint
    (create-locale-data)
    (clojure.java.io/writer locale-target))
  )

(comment
  (def test-zone 
    "Zone	Europe/Dublin	-0:25:00 -	LMT	1880 Aug  2
     -0:25:21 -	DMT	1916 May 21  2:00s
     -0:25:21 1:00	IST	1916 Oct  1  2:00s
     0:00	GB-Eire	%s	1921 Dec  6 
     0:00	GB-Eire	GMT/IST	1940 Feb 25  2:00s
     0:00	1:00	IST	1946 Oct  6  2:00s
     0:00	-	GMT	1947 Mar 16  2:00s
     0:00	1:00	IST	1947 Nov  2  2:00s
     0:00	-	GMT	1948 Apr 18  2:00s
     0:00	GB-Eire	GMT/IST	1968 Oct 27")


  (def test-zone
    "Zone	Europe/Minsk	1:50:16 -	LMT	1880
     1:50	-	MMT	1924 May  2 
     2:00	-	EET	1930 Jun 21
     3:00	-	MSK	1941 Jun 28
     1:00	C-Eur	CE%sT	1944 Jul  3
     3:00	Russia	MSK/MSD	1990
     3:00	-	MSK	1991 Mar 31  2:00s
     2:00	Russia	EE%sT	2011 Mar 27  2:00s
     3:00	-	+03")
  (extract-zones [:timezone (zone-parser test-zone :start :zone-definition)])
  (extract-zones europe)
  (extract-rules europe)
  (def data (extract-data europe))
  (get-zone "Europe/Belgrade")
  (get-zone "Africa/Algiers")
  (get-zone "Europe/Riga")
  (get-rule "C-Eur")
  (set
    (map
      (comp :rule-at)
      (reduce
        concat
        (-> timezone-data :rules vals))))
  (-> timezone-data :rules clojure.pprint/pprint)
  (filter #(clojure.string/starts-with? (key %) "Europe") (-> timezone-data :zones ))
  (doseq [[zone rules] (:zones timezone-data)]
    (println {zone (when-not (string? rules) (mapv :until rules))}))
  (->
    europe
    (clojure.string/replace #"#.*" "")
    clojure.string/split-lines
    (#(remove (comp empty? clojure.string/trim) %))
    (#(clojure.string/join "\n" %))
    (zone-parser :start :timezone)))
