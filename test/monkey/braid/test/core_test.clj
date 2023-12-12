(ns monkey.braid.test.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [aleph.http :as http]
            [monkey.braid.core :as sut]))

(deftest make-bot
  (testing "creates bot object using config"
    (is (some? (sut/make-bot {:bot-id (random-uuid)
                              :token "secret-token"})))))

(deftest send-message
  (with-redefs [http/post (fn [url args]
                              {:url url
                               :args args})]
    
    (testing "sends to http endpoint as configured in bot"
      (is (= "http://test/bots/message"
             (-> (sut/send-message
                  (sut/make-bot {:url "http://test"})
                  {:content "test message"})
                 :url))))

    (testing "uses bot id and token as basic auth credentials"
      (is (= ["test-bot-id" "test-token"]
             (-> (sut/send-message
                  (sut/make-bot {:bot-id "test-bot-id"
                                 :token "test-token"})
                  {:content "test message"})
                 :args
                 :basic-auth))))))
