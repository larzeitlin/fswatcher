(ns fs-watcher.rulesets
  "Rulesets are gathered together here so that there they are all available in a single namespace."
  (:require [fs-watcher.rulesets.ruleset1 :as rs1]))

(def ruleset-kw->rules
  {:ruleset1 rs1/rules

;; add new rulesets here

   })
