(ns mv-updater.processing
  (:require [clojure.data.xml :as x]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as z]
            [clojure.java.io :as io]))

(defn find-submodule-names
  [pom]
  (zx/xml-> pom :project :modules :module zx/text))

(defn update-versions
  [pom all-submodule-names new-version])

(defn collect-submodule-names
  [pom]
  (let [submodule-names (find-submodule-names pom)]
    ))

(defn load-xml
  [xml-file]
  (z/xml-zip (x/parse (io/reader xml-file))))

(defn save-xml
  [xml-zipper xml-file]
  (x/emit (z/root xml-zipper) (io/writer xml-file)))

(defn submodule-pom-file
  [pom-file submodule-name]
  (let [pom-dir (.getParent (io/file pom-file))
        submodule-file (io/file pom-dir submodule-name "pom.xml")]
    submodule-file))

(defn process
  [pom-file new-version & [all-submodule-names]]
  (let [pom (load-xml pom-file)
        all-submodule-names (or all-submodule-names (collect-submodule-names pom))
        submodule-names (find-submodule-names pom)
        submodule-pom-files (map submodule-pom-file submodule-names)]
    ; Apply version change in current pom
    (->
      pom
      (update-versions all-submodule-names new-version)
      (save-xml pom-file))
    ; Recursively apply version changes in submodules
    (doseq [submodule-pom-file submodule-pom-files]
      (process submodule-pom-file new-version all-submodule-names))))
