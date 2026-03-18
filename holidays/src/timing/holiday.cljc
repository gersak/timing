(ns timing.holiday
  (:refer-clojure :exclude [name])
  (:require
   [timing.core
    :refer [*holiday?*
            day-time-context
            time->value]]))

(defn dispatch [dispatch _] dispatch)

(defmulti is-holiday?
  "Multimethod for extending holiday? function. 'dispatch' parameter is used to 
  dispatch to proper implementation of multimethod. In most cases it should be locale or country
  but it can as well be religion or culture dispatch (key)words or any other data type"
  dispatch)

(defmethod is-holiday? :default [dispatch _]
  (let [message (str "Unknown dispatch " (pr-str dispatch) ". Are you sure that target multimethod implementation is loaded(required)?")]
    (throw
     #?(:clj (Exception. message)
        :cljs (js/Error. message)))))

(defn ? [dispatch date]
  (binding [*holiday?* nil]
    (is-holiday? dispatch (-> date time->value day-time-context))))

(defn name
  ([definition] (name :en definition))
  ([dispatch definition]
   (when-some [{f :name} definition]
     (when (fn? f)
       (f dispatch)))))

(def locale ::locale)
(def religion ::religion)
(def country ::country)

(derive locale :timing.core/holiday)
(derive religion :timing.core/holiday)
(derive country :timing.core/holiday)

(comment
  (name (? (timing.core/date 2028 12 26) :sl) :sl))
