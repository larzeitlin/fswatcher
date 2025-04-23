(ns
 localstack-utils
  "utils for setting up and interacting with localstack for integration testing"
  (:require
   [clojure.java.shell :refer [sh]]
   [cognitect.aws.credentials :as creds]
   [cognitect.aws.client.api :as aws]))

(defn localstack-down []
  (sh "docker-compose" "-f" "test/watchers/docker-compose.yml" "down" "-v"))

(defn localstack-up []
  (sh "docker-compose" "-f" "test/watchers/docker-compose.yml" "up" "-d"))

(def bucket-name "testbucket")

(def valid-connection-settings
  {:credentials {:access-key-id "test"
                 :secret-access-key "test"}
   :s3-client-settings {:api :s3
                        :region "us-east-1"
                        :endpoint-override {:protocol :http
                                            :hostname "localhost"
                                            :port  4566}}
   :bucket-name bucket-name})

(defn create-client [{:keys [s3-client-settings
                             credentials]
                      :as _connection-settings}]
  (aws/client (assoc s3-client-settings
                     :credentials-provider
                     (creds/basic-credentials-provider credentials))))

(defn op-successful? [resp]
  (not (:cognitect.anomalies/category resp)))

(defn create-bucket [client bucket-name]
  (aws/invoke client
              {:op :CreateBucket
               :ACL "public-read"
               :request {:Bucket bucket-name}}))

(defn bucket-exists?
  [client bucket-name]
  (let [resp (aws/invoke client {:op :HeadBucket :request {:Bucket bucket-name}})]
    (op-successful? resp)))

(defn list-bucket-objects [client bucket-name]
  (aws/invoke client {:op :ListObjectsV2 :request {:Bucket bucket-name}}))

(defn put-file [client bucket-name file-name]
  (aws/invoke client
              {:op :PutObject
               :request {:Bucket bucket-name :Key file-name
                         :Body (.getBytes "hello test")}}))

(defn wait-for-bucket-to-exist [client bucket-name max-wait-ms]
  (let [deadline (+ (System/currentTimeMillis) max-wait-ms)]
    (loop []
      (if (bucket-exists? client bucket-name)
        true
        (if (< (System/currentTimeMillis) deadline)
          (do (Thread/sleep 1000)
              (recur))
          (throw (ex-info "Timeout waiting for bucket to exist"
                          {:bucket bucket-name})))))))

(defn setup-localstack []
  (localstack-down)
  (localstack-up)
  (Thread/sleep 1000) ;; TODO: fix race condition in a more robust way
  (let [client (create-client valid-connection-settings)]
    (Thread/sleep 1000)
    (println (str "test bucket creation : "
                  (create-bucket client bucket-name)))
    (Thread/sleep 1000)
    (wait-for-bucket-to-exist client bucket-name 10000)))
