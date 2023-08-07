(ns speclj.report.junit-xml-reporter
  (:require [clojure.data.xml :as xml]
            [clojure.string :as s]
            [speclj-junit-xml.node :as node]
            [speclj.report.documentation :refer [level-of]]))

(defn -env [key] #?(:clj (System/getenv key)))

(defn report-filename []
  (let [report-path (-env "SPECLJ_REPORT_PATH")
        report-name (-env "SPECLJ_REPORT_NAME")
        report-path (if (s/blank? report-path) "speclj" report-path)
        report-name (if (s/blank? report-name) "speclj.xml" report-name)]
    (str report-path "/" report-name)))

(def results (atom {}))

(defn result-parent [result]
  (-> result .-characteristic .-parent deref))

(defn result-root [result]
  (loop [result result]
    (if (zero? (level-of result))
      result
      (recur (-> result .-parent deref)))))

(defn result-xml []
  (->> @results
      node/test-suites
      xml/sexp-as-element
      xml/emit-str
      (spit (report-filename))))

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
    (flush)
    (result-xml))

  (report-message [reporter message])                       ;; noop
  (report-pending [this result])                            ;; noop
  (report-error [this exception])                           ;; noop
  )

(defn new-junit-xml-reporter-reporter []
  (JunitXmlReporter.))