(ns mv-updater.log)

(def ^{:private true} date-formatter
  (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))

(defn format-date
  [date]
  (.format date-formatter date))

(defn now
  []
  (java.util.Date.))

(defn log [& args]
  (apply println (str "[" (format-date (now)) "]") args))
