(ns fs-watcher.watcher-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [fs-watcher.watcher :as subject]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.data :as data]))

(deftest apply-rules-test
  (testing "single rule"
    (let [action-target (atom :unchanged)
          rules [{:condition (fn [prev-gen new-gen]
                               (not= prev-gen new-gen))
                  :action (fn [_prev-gen _new-gen]
                            (reset! action-target :changed))}]]
      (testing "condition not met - action not triggered"
        (subject/apply-rules rules #{} #{})
        (is (= :unchanged @action-target)))
      (testing "condition met - action triggered"
        (subject/apply-rules rules #{} #{(io/file "a_new_file.txt")})
        (is (= :changed @action-target)))))

  (testing "multiple rules"
    (let [action-target (atom #{})
          rules [{:condition (fn [_prev-gen new-gen]
                               (->> new-gen
                                    (filter #(string/includes?
                                              (.getAbsolutePath %)
                                              "important_subdir"))
                                    seq))
                  :action (fn [_prev-gen _new-gen]
                            (swap! action-target conj :important-file))}
                 {:condition (fn [prev-gen new-gen]
                               (let [[deleted _ _] (data/diff prev-gen new-gen)]
                                 (seq deleted)))
                  :action (fn [_prev-gen _new-gen]
                            (swap! action-target conj :deletion))}]]
      (testing "no conditions met"
        (subject/apply-rules rules #{} #{})
        (is (= #{} @action-target)))

      (testing "one condition met"
        (subject/apply-rules rules #{(io/file "file_to_be_deleted.md")} #{})
        (is (= #{:deletion} @action-target)))

      (reset! action-target #{})
      
      (testing "both conditions met"
        (subject/apply-rules rules
                             #{(io/file "file_to_be_deleted.md")}
                             #{(io/file "dir/important_subdir/file.css")})
        (is (= #{:deletion :important-file} @action-target))))))

