(ns vura.holiday
  (:refer-clojure :exclude [name])
  (:require
   [vura.core
    :refer [day-time-context
            time->value]]))

(defn dispatch [dispatch _] dispatch)

(defmulti is-holiday?
  "Multimethod for extending holiday? function. 'dispatch' parameter is used to 
  dispatch to proper implementation of multimethod. In most cases it should be locale or country
  but it can as well be religion or culture dispatch (key)words or any other data type"
  dispatch)

(defmethod is-holiday? :default [dispatch _]
  (let [message (str "Unkonwn dispatch " (pr-str dispatch) ". Are you sure that target multimethod implementation is loaded(required)?")]
    (throw
     #?(:clj (Exception. message)
        :cljs (js/Error. message)))))

(defn ? [date dispatch]
  (is-holiday? dispatch (-> date time->value day-time-context)))

(defn name [definition dispatch]
  (when-some [{f :name} definition]
    (when (fn? f)
      (f dispatch))))

(def locale ::locale)
(def religion ::religion)
(def country ::country)

(derive locale :vura.core/holiday)
(derive religion :vura.core/holiday)
(derive country :vura.core/holiday)
