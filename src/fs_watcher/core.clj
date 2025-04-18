(ns fs-watcher.core
  (:require
   [fs-watcher.watchers.local-filesystem :as watchers.local]
   [fs-watcher.watcher :as watcher]
   [clojure.java.io :as io]
   [fs-watcher.rules :as rules]
   [clojure.tools.logging :as log]))

;; Erroring thread should not silently die
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error ex "Uncaught exception on" (.getName thread)))))

(def system*
  (atom {}))

(def watcher-type->constructor
  {:local watchers.local/->LocalFileSystemWatcher})

(defn main-loop
  [{:keys [interval connection-settings watcher-type]}]
  (reset! system* {:fs-state #{}
                   :interval interval
                   :connection-settings connection-settings
                   :watcher-type watcher-type
                   :rules rules/rules})
  (let [watcher-constructor (watcher-type->constructor watcher-type)
        fs (watcher-constructor system*)]
    (watcher/connect fs)
    (loop []
      (if (:disconnect! @system*)
        (watcher/disconnect fs)
        ;; else continue 
        (do (Thread/sleep interval)
            (watcher/poll fs)
            (recur))))))

(defn stop []
  (swap! system* assoc :disconnect! true))

(defn run
  "Runs the main-loop on another thread so we don't block the repl"
  [config]
  (future (main-loop config)))

(comment
  ;; for repl-driven experimentation

  (def example-config
    {:interval 1000
     :watcher-type :local
     :connection-settings
     {:root-directory "test/examplefs"}})

  (run example-config)

  (stop)

  @system*

  (spit "test/examplefs/newfile" "")

  (spit "test/examplefs/rule1" "")

  (io/delete-file "test/examplefs/newfile")

  (io/delete-file "test/examplefs/rule1"))

