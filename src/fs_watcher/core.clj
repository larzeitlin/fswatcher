(ns fs-watcher.core
  (:require [clara.rules :refer :all]
            [clojure.data :refer [diff]]
            [clojure.string :as string]
            [clojure.java.io :as io]))

(defprotocol FileSystem
  (connect [this] "connect to the tracked filesystem")
  (poll [this] "fetch the current state")
  (disconnect [this] "close the connection"))


(def file-system-atom
  (atom {}))

(defrecord LocalFileSystem [root-directory]
  FileSystem
  (connect [this]
    (let [potential-dir (io/file (:root-directory this))
          dir-exists? (and (.exists potential-dir) (.isDirectory potential-dir))]
      (swap! file-system-atom assoc :connected? dir-exists?)))

  (poll [this]
    (println "polling local fs")
    (println (diff (:state @file-system-atom)
                   (swap! ))
             )
    (set (.list (io/file (:root-directory this)))))

  (disconnect [_]
    ; nothing to do
    (swap! file-system-atom assoc
           :disconnect! true
           :connected? false)))


(def xx (->LocalFileSystem "test/examplefs"))

(connect xx)

(poll xx)

(disconnect xx)

(defn run []
  (let [fs (->LocalFileSystem "test/examplefs")]
    (loop []
      (if (:disconnect! @file-system-atom)
        (println "stopped!")
        (do
          (Thread/sleep 1000)
          (poll fs)
          (recur))))))


(def fs-gen0 #{})
(def fs-gen1 #{"my/dir/file_a.txt"})
(def fs-gen2 #{"my/dir/file_a.txt" "my/dir/dir2/file_b.txt"})
(def fs-gen3 #{"my/dir/dir2/file_b.txt" "my/dir/dir2/file_c.txt" })

(defn check-changes [prev-gen cur-gen]
  (let [[deleted added _] (diff prev-gen cur-gen)]
    {:deleted deleted
     :added added}))

(check-changes fs-gen0 fs-gen1)

;; behaviours
(defprotocol Behaviours
  (print-filepath [this] "Method to print filepath"))


;; A record per filesystem type
(defrecord File [path]
  Behaviours
  (print-filepath [this] (println (str "file: " (:path this)))))


;; rules
(defrule rule-a
  "Behaviour A is triggered if a new file as a given prefix"
  [?file <- File (string/starts-with? path "/some/subdirectory")]
  => (print-filepath ?file))

(-> (mk-session)
    (insert (->File "/someother/dir/file.txt")
            (->File "/some/subdirectory.file2.txt"))
    (fire-rules))


