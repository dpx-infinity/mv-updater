(ns mv-updater.processing
  (:require [clojure.data.xml :as x]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as z]
            [clojure.java.io :as io])
  (:require [mv-updater.config :as c]
            [mv-updater.log :as log]))

(defn load-xml
  "Loads given XML file from file and wraps it into zipper."
  [xml-file]
  (z/xml-zip (x/parse (io/reader xml-file))))

(defn save-xml
  "Saves given XML zipper to the file."
  [xml-zipper xml-file]
  (with-open [w (io/writer xml-file)]
    (x/indent (z/root xml-zipper) w)))

(defn submodule-pom-file
  "Returns valid file object naming submodule pom.xml file given parent pom file object and submodule name."
  [pom-file submodule-name]
  (let [pom-dir (.getParent (io/file pom-file))
        submodule-file (io/file pom-dir submodule-name "pom.xml")]
    submodule-file))

;; TODO: create parent artifact name finding function

(defn find-submodule-names
  "Returns names of all submodules of given pom. Submodules are defined like this:
  <project>
    ...
    <modules>
      <module>module-1</module>
      <module>subdir/module-2</module>
    </modules>
    ...
  </project>
  So, for the XML above this function will return [\"module-1\" \"subdir/module-2\"]."
  [pom]
  (zx/xml-> pom :modules :module zx/text))

(defn find-this-artifact-name
  "Returns artifact name of the given pom. Artifact name is defined like this:
  <project>
    ...
    <artifactId>artifact-name</artifactId>
  </project>
  So, for the XML above this function will return \"artifact-name\"."
  [pom]
  {:name (zx/xml1-> pom :artifactId zx/text)
   :group (zx/xml1-> pom :groupId zx/text)})

(defn get-project-version-node
  [pom]
  (zx/xml1-> pom :version))

(defn get-project-version
  [pom]
  (-> pom get-project-version-node zx/text))

(defn get-parent-version-node
  [pom]
  (zx/xml1-> pom :parent :version))

(defn get-parent-version
  [pom]
  (-> pom get-parent-version-node zx/text))

(defn update-project-version
  "Updates version number in project artifact declaration, if the project artifact name is present in
  all-module-names list and if it is not excluded in configuration."
  [pom all-module-names new-version]
  (let [name (find-this-artifact-name pom)]
    (if (and
          (some #{name} all-module-names)
          (not (c/excluded? name))
          (get-project-version-node pom))
      (-> pom
        get-project-version-node
        (z/edit assoc :content [new-version])
        z/up)
      pom)))

(defn update-parent-version
  [pom all-module-names new-version])

(defn update-dependencies-versions
  [pom all-module-names new-version])

(defn update-versions
  "Updates version numbers in the given XML tree according to the configuration"
  [pom all-module-names new-version]
  (-> pom
    (update-project-version all-module-names new-version)
    (update-parent-version all-module-names new-version)
    (update-dependencies-versions all-module-names new-version)))

(defn collect-submodule-artifact-names
  "Recursively finds artifact names of the given pom and all of its submodules. That is, it finds all submodule names
  of the given pom and then asks them for their artifact names, returning their answers plus its own name."
  [pom-file]
  (let [pom (load-xml pom-file)
        this-artifact-name (find-this-artifact-name pom)
        submodule-names (find-submodule-names pom)
        submodule-pom-files (map (partial submodule-pom-file pom-file) submodule-names)
        submodule-artifact-names (mapcat collect-submodule-artifact-names submodule-pom-files)]
    (conj submodule-artifact-names this-artifact-name)))

(defn save-new-pom
  "Save the pom to the pom-file, creating backup if necessary."
  [pom pom-file]
  (let [new-pom-file
        (if (c/overwrite-enabled)
          pom-file
          (io/file (str pom-file (c/new-suffix))))
        backup-pom-file
        (io/file (str pom-file c/backup-suffix))]
    ; When overwrite is enabled, backup the pom
    (when (c/overwrite-enabled)
      (log/log "Backing up" (str "'" pom-file "'") "to" (str "'" backup-pom-file "'"))
      (org.apache.commons.io.FileUtils/copyFile pom-file backup-pom-file))
    ; Save the file
    (log/log "Saving" (str "'" new-pom-file "'"))
    (save-xml pom new-pom-file)))

(defn process
  "Processing entry function, walks around pom tree, replacing versions where needed and saving poms back to files.
  Calls itself to recurse deeper in the tree."
  ([pom-file new-version] (process pom-file new-version nil))
  ([pom-file new-version all-artifact-names]
    (log/log "Processing" (str "'" pom-file "'"))
    (let [pom (load-xml pom-file)
          all-artifact-names
          (or all-artifact-names
            (do
              (log/log "Collecting all submodule artifact names...")
              (doall (map #(do (log/log "Found" %) %) (collect-submodule-artifact-names pom-file)))))
          submodule-names (find-submodule-names pom)
          submodule-pom-files (map (partial submodule-pom-file pom-file) submodule-names)]
      ; Apply version change in current pom
      (-> pom
        (update-versions all-artifact-names new-version)
        (save-new-pom pom-file))
      ; Recursively apply version changes in submodules
      (doseq [submodule-pom-file submodule-pom-files]
        (process submodule-pom-file new-version all-artifact-names)))))
