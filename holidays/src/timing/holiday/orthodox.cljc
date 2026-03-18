(ns timing.holiday.orthodox)

(defn orthodox-easter [{:keys [year]}]
  (let [a (mod year 4)
        b (mod year 7)
        c (mod year 19)
        d (mod (+ (* 19 c) 15) 30)
        e (mod (- (+ (* 2 a) (* 4 b) 34) d) 7)
        month (quot (+ d e 114) 31)
        day (+ 14 (mod (+ d e 114) 31))
        day-overload (quot day 30)]
    (if (pos? day-overload)
      {:year year :month (inc month) :day-in-month (mod day 30)}
      {:year year :month month :day-in-month day})))
