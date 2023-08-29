(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.cleancoders/speclj-junit-xml)
(def version (format "1.0.1"))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/copy-dir {:src-dirs ["src/clj" "src/cljc"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))