(ns fs-watcher.watchers.s3
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.credentials :as creds]
            [fs-watcher.watcher :as watcher]
            [clojure.java.io :as io]))

(defn make-s3-client [{:keys [s3-client-settings
                              credentials]
                       :as _connection-settings}]
  (aws/client (assoc s3-client-settings
                     :credentials-provider
                     (creds/basic-credentials-provider credentials))))

(defn list-bucket-objects [client bucket-name]
  (aws/invoke
   client
   {:op :ListObjectsV2
    :request {:Bucket bucket-name}}))

(defrecord S3BucketWatcher [system*]
  watcher/FileSystemWatcher
  (connect [this]
    (watcher/wrap-connect
     this
     (fn [{:keys [connection-settings]}]
       (let [client (make-s3-client connection-settings)
             list-bucket-results (list-bucket-objects
                                  client
                                  (:bucket-name connection-settings))]
         (swap! system* assoc :s3-client client)
         (-> list-bucket-results :Name boolean) ;bucket name included when successful 
         ))))

  (poll [this]
    (watcher/wrap-poll
     this
     (fn [{:keys [connection-settings s3-client]}]
       (let [{:keys [bucket-name]} connection-settings
             resp (list-bucket-objects s3-client bucket-name)]
         ;;TODO: add error handling
         (some->> resp
                  :Contents
                  (map :Key)
                  (map #(io/file %))
                  set)))))

  (disconnect [this]
    (watcher/wrap-disconnect
     this
     ;; no special disconnection behaviour
     (constantly true))))



