(ns speclj-junit-xml.node
  (:require [c3kit.apron.corec :as ccc]
            [c3kit.apron.time :as time]
            [clojure.string :as s]
            [speclj.reporting :refer [stack-trace-str tally-time]]
            [speclj.results :refer [categorize]]
            [speclj.report.documentation :refer [level-of]]
            [speclj.platform :refer [error-message failure-source]]))

(def characteristic-id (atom 0))
(defn next-node-id [] (swap! characteristic-id inc))
(defn fmt-time [time]
  (ccc/formats "%.3f" (+ 0.0 time)))

(defn nested-desc-name [desc]
  (loop [desc desc name (str)]
    (if (zero? (level-of desc))
      name
      (recur (-> desc .-parent deref) (s/trim (str (.-name desc) " " name))))))

(defn result-parent [result]
  (-> result .-characteristic .-parent deref))
(defn result-name [result desc]
  (s/trim (str (nested-desc-name desc) " " (-> result .-characteristic .-name))))

(defn test-case
  ([result] (test-case result nil))
  ([result children]
   (let [node [:testcase {:id   (next-node-id)
                          :name (result-name result (result-parent result))
                          :time (fmt-time (.-seconds result))}]]
     (if children
       (conj node children)
       node))))

(defn fail-case [result]
  (let [failure (.-failure result)
        stack-trace (str (failure-source failure) "\n" (stack-trace-str failure))]
    (test-case result [:failure {:message (error-message failure)
                                 :type    "failure"}
                       stack-trace])))

(defmulti result-node type)

(defmethod result-node speclj.results.PassResult [result]
  (test-case result))

(defmethod result-node speclj.results.FailResult [result]
  (fail-case result))

(defmethod result-node speclj.results.ErrorResult [result]
  (fail-case result))

(defn test-suite [name desc-results]
  (let [tallies (categorize desc-results)]
    (into
      [:testsuite {:id       (next-node-id)
                   :name     name
                   :tests    (-> tallies vals flatten count str)
                   :failures (-> tallies :fail count str)
                   :time     (fmt-time (tally-time desc-results))}]
      (for [result desc-results]
        (result-node result)))))

(defn test-suites [results]
  (let [tallies (map #(-> % categorize vals flatten count) (vals results))
        failures (map #(-> % categorize :fail count) (vals results))
        times (map tally-time (vals results))
        now (time/now)]
    (into
      [:testsuites {:id       (time/unparse :dense now)
                    :name     (time/unparse :iso8601 now)
                    :tests    (str (reduce + tallies))
                    :failures (str (reduce + failures))
                    :time     (fmt-time (reduce + times))}]
      (for [result results]
        (test-suite (-> result key .-name) (val result))))))