(ns fs-watcher.rulesets.ruleset1
  (:require [clojure.data :refer [diff]]
            [clojure.string :as string]))

(def rules
  [
   ;; a rule that prints rule 1 if any file is added that has rule1 in the path 
   {:condition (fn [prev-gen new-gen]
                 (let [[_deleted added _] (diff prev-gen new-gen)]
                   (->> added
                        (filter #(string/includes? (.getAbsolutePath %) "rule1"))
                        seq)))
    :action (fn [_ _] (println "rule 1"))}
   ])
