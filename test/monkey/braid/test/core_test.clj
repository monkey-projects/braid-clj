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

(deftest env->config
  (testing "creates default config on empty env"
    (is (map? (sut/env->config {}))))

  (testing "adds bot id and token"
    (is (= {:bot-id "test-bot"
            :token "test-token"}
           (-> {:braid-bot-id "test-bot"
                :braid-bot-token "test-token"}
               (sut/env->config)
               :bot
               (select-keys [:bot-id :token])))))

  (testing "uses url if specified"
    (is (= "http://test-url"
           (-> {:braid-url "http://test-url"}
               (sut/env->config)
               :bot
               :url))))

  (testing "uses default url if unspecified"
    (is (= sut/default-url
           (-> {}
               (sut/env->config)
               :bot
               :url))))

  (testing "adds http port"
    (is (= 8080 (-> {:http-port 8080}
                    (sut/env->config)
                    :http
                    :port)))))
