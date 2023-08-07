(ns speclj.report.junit-xml-reporter-spec
  (#?(:clj :require :cljs :require-macros)
   [speclj.core :refer [describe it should= should-be-nil should-contain with]])
  (:require [clojure.data.xml :as xml]
            [speclj-junit-xml.node :as node]
            [speclj.core #?(:clj :refer :cljc :refer-macros)
             [describe it should= before -new-failure]]
            [speclj.report.junit-xml-reporter :as sut]
            [speclj.reporting :refer [report-description report-pass report-fail]]
            [speclj.components :refer [new-description new-characteristic install]]
            [speclj.results :refer [pass-result fail-result]]))

(def desc (new-description "desc 1" false "ns.1"))
(def desc3 (new-description "desc 2" false "ns.2"))
(def nested-desc (new-description "nested (parent)" false "ns.1"))
(def nested-2-desc (new-description "nested (child)" false "ns.1"))
(install nested-desc desc)
(install nested-2-desc nested-desc)

(defn passing-characteristic [desc]
  (new-characteristic "passing" desc #() false))
(defn failing-characteristic [desc]
  (new-characteristic "failing" desc #() false))

(def pass1 (pass-result (passing-characteristic desc) 1))
(def pass2 (pass-result (passing-characteristic desc) 2))
(def pass3 (pass-result (passing-characteristic desc3) 3))
(def pass-nested (pass-result (passing-characteristic nested-desc) 3))
(def pass-nested-2 (pass-result (passing-characteristic nested-2-desc) 4))
(def fail1 (fail-result (failing-characteristic desc) 1 (-new-failure "FAIL")))
(def fail2 (fail-result (failing-characteristic desc) 2 (-new-failure "FAIL")))
(def fail-nested (fail-result (failing-characteristic nested-desc) 3 (-new-failure "FAIL")))
(def fail-nested-2 (fail-result (failing-characteristic nested-2-desc) 4 (-new-failure "FAIL")))

(describe "Speclj Junit XML Reporter"
  (with reporter (sut/new-junit-xml-reporter-reporter))
  (before (reset! sut/results {})
    (reset! node/characteristic-id 0))

  (describe "reports"
    (it "only first level description"
      (report-description @reporter desc)
      (report-description @reporter nested-desc)

      (should= [] (get @sut/results desc))
      (should-be-nil (get @sut/results nested-desc)))

    (it "passing test"
      (report-pass @reporter pass1)
      (should-contain pass1 (get @sut/results desc)))

    (it "failing test"
      (report-fail @reporter fail1)
      (should-contain fail1 (get @sut/results desc)))

    (it "runs"
      (report-pass @reporter pass1)
      (report-fail @reporter fail1)
      (let [expected (-> @sut/results node/test-suites xml/sexp-as-element
                       xml/emit-str println with-out-str)]
        (reset! sut/results {})
        (reset! node/characteristic-id 0)
        (report-pass @reporter pass1)
        (report-fail @reporter fail1)

        (should= expected
          (with-out-str (sut/result-xml)))))))