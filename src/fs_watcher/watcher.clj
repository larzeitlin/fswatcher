(ns fs-watcher.watcher)

(defn apply-rules
  "Applies rules based on a comparison of the previous
  and new filesystem state."
  [rules prev-gen new-gen]
  (doseq [{:keys [condition action]} rules]
    (when (condition prev-gen new-gen)
      (action prev-gen new-gen))))

(defn wrap-connect
  "Wrap a connect-fn to handle updating system.
  connect-fn should take the derefed system
  return a boolean indicating success of connection."
  [{:keys [system*]} connect-fn]
  (let [connected? (connect-fn @system*)]
    (print connected?)
    (println "connecting to filesystem")
    (swap! system* assoc
           :connected? connected?
           :disconnect! false)
    connected?))

(defn wrap-poll
  "Wraps a poll-fn to handle updating system and
  application of rules. poll-fn should take the
  derefed system and return a set of java.io.Files
  representing the new state of the file system."
  [{:keys [system*]} poll-fn]
  (let [{:keys [rules fs-state]} @system*
        new-gen (poll-fn @system*)
        prev-gen fs-state]
    (apply-rules rules prev-gen new-gen)
    (swap! system* assoc :fs-state new-gen)
    new-gen))

(defn wrap-disconnect
  "Wraps a disconnect-fn to handle updating system.
  disconnect-fn should take the dereffed system and
  return a boolean indicating success of disconnection."
  [{:keys [system*]} disconnect-fn]
  (let [disconnected? (disconnect-fn @system*)]
    (println "disconnecting from filesystem")
    (swap! system* assoc
           :disconnect! true
           :connected? (not disconnected?))

    disconnected?))

(defprotocol FileSystemWatcher
  (connect [this] "connect to the tracked filesystem.")
  (poll [this] "fetch the current state.")
  (disconnect [this] "close the connection."))
