(ns fs-watcher.rules
  "Rules define actions that are taken under certain conditions by comparison
  of the previous and new generations of the filesystem.

  A rule is a map of :condition and :action. Both are functions that take the
  previous and new generation of the filesystem. prev-gen and new-gen are
  sets of java.io.Files."
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

