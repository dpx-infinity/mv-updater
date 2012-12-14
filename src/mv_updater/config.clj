(ns mv-updater.config
  (:require [clj-yaml.core :as y])
  (:require [mv-updater.log :as log]))

(def ^:private default-config
  {:places [:project-version :dependency-version :parent-version]})

(defn load-config
  [file-name]
  (try
    (let [cfg (-> (slurp file-name) (y/parse-string))]
      (assoc cfg :places (map keyword (:places cfg))))
    (catch java.io.IOException e
      (log/log "Config loading failed:" (.toString e))
      default-config)))
