(ns mv-updater.processing-test
  (:require [mv-updater.processing :as p]
            [mv-updater.config :as c]
            [clojure.data.xml :as x]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as z])
  (:use clojure.test))

(defn xml-string
  [s]
  (z/xml-zip (x/parse-str (.trim s))))

(def xml-1
  (xml-string
    "<project>
       <groupId>group</groupId>
       <artifactId>artifact-1</artifactId>
       <version>123</version>
       <modules>
         <module>module-1</module>
         <module>deeper/module-2</module>
       </modules>
     </project>"))

(def xml-1-name
  {:name "artifact-1" :group "group"})

(def xml-2
  (xml-string
    "<project>
       <groupId>group</groupId>
       <artifactId>artifact-1</artifactId>
       <modules>
         <module>module-1</module>
         <module>deeper/module-2</module>
       </modules>
     </project>"))

(def xml-2-name
  {:name "artifact-1" :group "group"})

(deftest finding-submodule-names
  (testing "two module names are loaded"
    (is (= ["module-1" "deeper/module-2"] (p/find-submodule-names xml-1)))))

(deftest finding-artifact-name
  (testing "artifact name is loaded"
    (is (= {:name "artifact-1" :group "group"} (p/find-this-artifact-name xml-1)))))

(deftest updating-project-version
  (testing "version is successfuly changed"
    (is (= "789.3"
          (-> (p/update-project-version xml-1 [xml-1-name] "789.3")
            p/get-project-version))))
  (testing "version is not changed because of configuration exclusion"
    (is (= "123"
          (-> (c/with-config {:exclusions [xml-1-name]}
                (p/update-project-version xml-1 [xml-1-name] "789.3"))
            p/get-project-version))))
  (testing "version is not changed because of empty module names list"
    (is (= "123"
          (-> (p/update-project-version xml-1 [] "789.3")
            p/get-project-version))))
  (testing "version is not changed because it is not present"
    (is (not
          (-> (p/update-project-version xml-2 [xml-2-name] "789.3")
            p/get-project-version-node)))))
