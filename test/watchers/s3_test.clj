(ns watchers.s3-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [fs-watcher.watchers.s3 :as subject]
            [fs-watcher.watcher :as watcher]
            [localstack-utils :as ls-utils]
            [clojure.java.io :as io]))

(defn localstack-fixture [f]
  (ls-utils/setup-localstack)
  (f)
  (ls-utils/localstack-down))

(use-fixtures :each localstack-fixture)

(deftest ^:integration test-S3-client
  (testing "invalid client config fails to make an s3 client"
    (is
     (= :expected-error
        (try (subject/make-s3-client {})
             (catch AssertionError _e
               :expected-error)))))
  (testing "valid client config makes an s3 client"
    (let [valid-client (subject/make-s3-client ls-utils/valid-connection-settings)]
      (is valid-client)
      (testing "list objects on a non existant bucket fails"
        (is (= "NoSuchBucket"
               (some->> (ls-utils/list-bucket-objects valid-client "notabucket")
                        :Error
                        :Code))))

      (testing "list objects on an existant empty bucket is successful"
        (is (= 0
               (->> (ls-utils/list-bucket-objects valid-client ls-utils/bucket-name)
                    :KeyCount))))
      (testing "list objects shows an added file"
        (let [file-name "test_file.txt"]
          (ls-utils/put-file valid-client ls-utils/bucket-name file-name)
          (let [resp (ls-utils/list-bucket-objects valid-client ls-utils/bucket-name)]
            (is (= file-name
                   (->> resp :Contents first :Key)))))))))

(deftest ^:integration test-s3-watcher
  ;; test setup
  (let [client (subject/make-s3-client ls-utils/valid-connection-settings)]

    ;; watcher setup
    (let [system* (atom {:connection-settings ls-utils/valid-connection-settings})
          watcher (subject/->S3BucketWatcher system*)]

      ;; tests
      (testing "watcher successfully connects."
        (is (true? (watcher/connect watcher))))

      (testing "watcher can poll empty bucket"
        (is (nil? (watcher/poll watcher))))

      (testing "poll after adding a file shows the file present"
        (let [file-name "test_file.txt"]
          (ls-utils/put-file client ls-utils/bucket-name file-name)
          (is (= #{(io/file file-name)}
                 (watcher/poll watcher)))))

      (testing "watecher successfully disconnects"
        (is (true? (watcher/disconnect watcher)))))))

