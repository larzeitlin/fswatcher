(ns fs-watcher.core
  (:require
   [fs-watcher.watchers.local-filesystem :as watchers.local]
   [fs-watcher.watchers.s3 :as watchers.s3]
   [fs-watcher.watcher :as watcher]
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
  {:local watchers.local/->LocalFileSystemWatcher
   :s3 watchers.s3/->S3BucketWatcher})

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
  "Runs the main-loop on another thread so we don't block the REPL."
  [config]
  (future (main-loop config)))
