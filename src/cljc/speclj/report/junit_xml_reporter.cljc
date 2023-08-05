(ns speclj.report.junit-xml-reporter
  (:require [clojure.data.xml :as xml]
            [speclj.report.documentation :refer [level-of]]
            [speclj-junit-xml.node :as node]))

(def results (atom {}))

(defn result-parent [result]
  (-> result .-characteristic .-parent deref))
(defn result-root [result]
  (loop [result result]
    (if (zero? (level-of result))
      result
      (recur (-> result .-parent deref)))))
(defn result-xml []
  (-> @results
    node/test-suites
    xml/sexp-as-element
    xml/emit-str
    println))

(deftype JunitXmlReporter []
  speclj.reporting/Reporter

  (report-description [_ description]
    (when (zero? (level-of description))
      (swap! results assoc description [])))

  (report-pass [_ result]
    (swap! results update-in [(result-root (result-parent result))] conj result))

  (report-fail [_ result]
    (swap! results update-in [(result-root (result-parent result))] conj result))

  (report-runs [_ _]
    (result-xml)))

(defn new-junit-xml-reporter-reporter []
  (JunitXmlReporter.))