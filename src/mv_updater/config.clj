(ns mv-updater.config
  (:require [clj-yaml.core :as y]
            [clojure.set :as s]
            [clojure.string :as st])
  (:require [mv-updater.log :as log]))

(def ^:dynamic *config* nil)

(def ^:private default-config
  {:config {:overwrite false :new-suffix ".new" :backup-suffix ".bak"} :places #{:project :dependency :parent}})

(defn overwrite-enabled
  []
  (:overwrite (:config *config*)))

(defn backup-suffix
  []
  (:backup-suffix (:config *config*)))

(defn new-suffix
  []
  (:new-suffix (:config *config*)))

(defn place-enabled?
  [place]
  ((:places *config*) place))

(defn- process-exclusions
  "Converts a seq of strings in form \"group-name/artifact-name\" to the seq of maps in form
  {:name \"artifact-name\" :group \"group-name\"}. If no slash is present in the string, it is used
  both for name and group. This is standard leiningen notation for artifacts."
  [es]
  (->> es
    (map #(st/split % #"/" 2))
    (map (fn [[p1 p2]] (if p2 {:group p1 :name p2} {:group p1 :name p1})))))

(defn load-config
  [file-name]
  (try
    (let [cfg (-> (slurp file-name) (y/parse-string))]
      (assoc cfg
        :places (s/union (:places default-config) (into #{} (map keyword (:places cfg))))
        :config (merge (:config default-config) (:config cfg))
        :exclusions (process-exclusions (:exclusions cfg))))
    (catch java.io.IOException e
      (log/log "Config loading failed:" (str e))
      default-config)))

(defmacro with-config
  [cfg & body]
  `(binding [*config* ~cfg]
     ~@body))
