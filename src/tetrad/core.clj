(ns tetrad.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as string]))
(defn add-activity-to-summary-by-people
  [summary {:keys [social-workers day activity-duration] :as activity}]
  (doall (reduce #(if (%1 %2)
                    (update-in %1 [%2] (fn [ps] (assoc ps day activity-duration)))
                    (assoc %1 %2 (sorted-map day activity-duration)))
                 summary
                 social-workers)))

(defn -main [filename]
  (with-open [reader (io/reader filename)]
    (doall
      (->> (csv/read-csv reader)
           (drop 1)
           ;;(take 10)
           (map #(zipmap [:date :month :weekday :day
                          :activity-start-time :activity-duration
                          :contacts-male-qty :contacts-male-codes :contacts-initial-male-qty :contacts-initial-male-codes
                          :contacts-female-qty :contacts-female-codes :contacts-initial-female-qty :contacts-initial-female-codes
                          :syringes-qty :syr-1 :syr-2-3 :syr-5 :syr-10 :syr-20 :syr-collected :needles-qty :napkins-qty :condoms-qty :misc-outlet :info-outlet
                          :cons-aids :cons-hep-c :cons-hep-b :cons-std :cons-tub :cons-addic :cons-pio :cons-overdose :cons-juridical :cons-misc
                          :social-workers :naloxon-outlet :saved-lives :naloxon-info :heart-energy :descritpion :place :volunteers-qty :media-contacts :no-code-male-qty :no-code-female-qty :hiv-tests-qty :hiv-tests-positive-qty] %))
           (map #(select-keys % [:date :month :day :social-workers :place :activity-duration]))
           (map #(update-in % [:date] (fn [date] (f/parse (f/formatter "dd.MM.YYYY H:mm:ss") date))))
           (map #(update-in % [:social-workers] (fn [workers] (string/split workers #",\s"))))
           (map #(update-in % [:day] (fn [d] (read-string d))))
           (map #(update-in % [:activity-duration] (fn [ad] (when-let [d (re-find #"^\d+" ad)] (read-string d)))))
           (filter #(and
                      (= "Ноябрь" (:month %))
                      (= 2017 (-> % :date t/year))))
           (reduce add-activity-to-summary-by-people {})
           (map (fn [[name days]]
                  (println (str name ": " (string/join "/" (map (fn [[day hour]] (str day "-" hour "ч")) days))))
                  (println "Итого: " (reduce + (vals days)) "ч\n")))
           ))))
