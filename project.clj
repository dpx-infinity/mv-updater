(defproject dpx-infinity/mv-updater "0"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/data.xml "0.0.5"]
                 [clj-yaml "0.4.0"]
                 [commons-io "2.4"]]
  :exclusions [org.clojure/clojure]

  :aot :all
  :warn-on-reflection true

  :source-paths ["src"]
  :resources-paths ["resources"]
  :test-paths ["test"]
  :test-resources-paths ["test-resources"]

  :omit-source true
  :target-path "out"
  :compile-path "out/classes"
  :library-path "out/lib")
