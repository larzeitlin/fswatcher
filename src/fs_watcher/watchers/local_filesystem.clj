(ns fs-watcher.watchers.local-filesystem
  (:require [fs-watcher.watcher :as watcher]
            [clojure.java.io :as io]))

(defrecord LocalFileSystemWatcher [system*]

  watcher/FileSystemWatcher
  (connect [this]
    (watcher/wrap-connect
     this
     (fn [{:keys [connection-settings]}]
       ;; for local fs simply check that the directory exists
       (let [potential-dir (-> connection-settings :root-directory io/file)
             dir-exists? (and (.exists potential-dir)
                              (.isDirectory potential-dir))]
         dir-exists?))))

  (poll [this]
    (watcher/wrap-poll
     this
     (fn [{:keys [connection-settings]}]
       ;; list files excluding directories
       (->>
        connection-settings
        :root-directory
        io/file
        file-seq
        (filter #(.isFile %))
        set))))

  (disconnect [this]
    (watcher/wrap-disconnect
     this
     ;; no special disconnection behaviour
     (constantly true))))
