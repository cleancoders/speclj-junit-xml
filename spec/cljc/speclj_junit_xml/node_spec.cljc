(ns speclj-junit-xml.node-spec
  (#?(:clj :require :cljs :require-macros)
   [speclj.core :refer [describe it  should= should-contain with]])
  (:require [c3kit.apron.time :as time]
            [speclj.core #?(:clj :refer :cljc :refer-macros)
             [describe it should= should-have-invoked before
              with-stubs stub redefs-around]]
            [speclj-junit-xml.node :as sut]
            [speclj.report.junit-xml-reporter :as xml-reporter]
            [speclj.reporting :refer [report-description report-pass
                                      report-fail stack-trace-str]]
            [speclj.platform :refer [failure-source]]
            [speclj.report.junit-xml-reporter-spec :refer [desc nested-desc desc3 pass3
                                                           pass1 pass2 pass-nested pass-nested-2
                                                           fail1 fail2 fail-nested fail-nested-2]]))

(defn get-attr [node key] (get (second node) key))

(describe "XML nodes"
  (with-stubs)
  (with reporter (xml-reporter/new-junit-xml-reporter-reporter))
  (before (reset! sut/characteristic-id 0)
    (reset! xml-reporter/results {}))

  (describe "created for"
    (describe "passing result"

      (it "with title"
        (should-contain :testcase (sut/result-node pass2)))

      (it "with unique id"
        (should= 1 (-> (sut/result-node pass2) (get-attr :id)))
        (should= 2 (-> (sut/result-node pass1) (get-attr :id))))

      (it "with name"
        (should= "passing"
          (-> (sut/result-node pass1) (get-attr :name))))

      (it "with nested description"
        (should= "nested (parent) passing"
          (-> (sut/result-node pass-nested) (get-attr :name)))
        (should= "nested (parent) nested (child) passing"
          (-> (sut/result-node pass-nested-2) (get-attr :name)))))

    (describe "failing result"
      (redefs-around [stack-trace-str (stub :stack-trace)
                      failure-source (stub :failure-src)])

      (it "with title"
        (should-contain :testcase (sut/result-node fail1)))

      (it "with unique id"
        (should= 1 (-> (sut/result-node fail2) (get-attr :id)))
        (should= 2 (-> (sut/result-node fail1) (get-attr :id))))

      (it "with name"
        (should= "failing"
          (-> (sut/result-node fail1) (get-attr :name))))

      (it "with nested description"
        (should= "nested (parent) failing"
          (-> (sut/result-node fail-nested) (get-attr :name)))
        (should= "nested (parent) nested (child) failing"
          (-> (sut/result-node fail-nested-2) (get-attr :name))))

      (it "with failure"
        (should-contain [:failure {:message "FAIL" :type "failure"} "\n"]
          (sut/result-node fail1))
        (should-have-invoked :stack-trace {:times 1})
        (should-have-invoked :failure-src {:times 1})))

    (describe "test suite"
      (redefs-around [stack-trace-str (stub :stack-trace)
                      failure-source (stub :failure-src)])
      (it "with single, passing result"
        (report-description @reporter desc)
        (report-pass @reporter pass1)
        (let [desc-name (.-name desc)
              desc-results (get @xml-reporter/results desc)]
          (should= [:testsuite {:id 1 :name desc-name :tests "1" :failures "0" :time "1.000"}
                    [:testcase {:id 2 :name "passing" :time "1.000"}]]
            (sut/test-suite desc-name desc-results))))

      (it "with passing and failing result"
        (report-description @reporter desc)
        (report-pass @reporter pass1)
        (report-fail @reporter fail1)
        (let [desc-name (.-name desc)
              desc-results (get @xml-reporter/results desc)]
          (should= [:testsuite {:id 1 :name desc-name :tests "2" :failures "1" :time "2.000"}
                    [:testcase {:id 2 :name "passing" :time "1.000"}]
                    [:testcase {:id 3 :name "failing" :time "1.000"}
                     [:failure {:message "FAIL", :type "failure"} "\n"]]]
            (sut/test-suite desc-name desc-results))))

      (it "with nested results"
        (report-description @reporter desc)
        (report-description @reporter nested-desc)
        (report-pass @reporter pass-nested)
        (report-pass @reporter pass-nested-2)
        (let [desc-name (.-name desc)
              desc-results (get @xml-reporter/results desc)]
          (should= [:testsuite {:id 1 :name desc-name :tests "2" :failures "0" :time "7.000"}
                    [:testcase {:id 2 :name "nested (parent) passing" :time "3.000"}]
                    [:testcase {:id 3 :name "nested (parent) nested (child) passing" :time "4.000"}]]
            (sut/test-suite desc-name desc-results)))))

    (describe "test suites"
      (redefs-around [time/now (stub :now {:return (time/now)})
                      stack-trace-str (stub :stack-trace)
                      failure-source (stub :failure-src)])

      (it "with single test suite"
        (report-description @reporter desc)
        (report-pass @reporter pass1)
        (let [desc-name (.-name desc)]
          (should= [:testsuites {:id       (time/unparse :dense (time/now))
                                 :name     (str (time/unparse :iso8601 (time/now)))
                                 :tests    "1"
                                 :failures "0"
                                 :time     "1.000"}
                    [:testsuite {:id 1 :name desc-name :tests "1" :failures "0" :time "1.000"}
                     [:testcase {:id 2 :name "passing" :time "1.000"}]]]
            (sut/test-suites @xml-reporter/results))))

      (it "with multiple test suites"
        (report-description @reporter desc)
        (report-description @reporter desc3)
        (report-pass @reporter pass1)
        (report-fail @reporter fail1)
        (report-pass @reporter pass3)
        (should= [:testsuites {:id       (time/unparse :dense (time/now))
                               :name     (str (time/unparse :iso8601 (time/now)))
                               :tests    "3"
                               :failures "1"
                               :time     "5.000"}
                  [:testsuite {:id 1 :name "desc 1" :tests "2" :failures "1" :time "2.000"}
                   [:testcase {:id 2 :name "passing" :time "1.000"}]
                   [:testcase {:id 3 :name "failing" :time "1.000"}
                    [:failure {:message "FAIL", :type "failure"} "\n"]]]
                  [:testsuite {:id 4, :name "desc 2", :tests "1", :failures "0", :time "3.000"}
                   [:testcase {:id 5, :name "passing" :time "3.000"}]]]
          (sut/test-suites @xml-reporter/results))))))