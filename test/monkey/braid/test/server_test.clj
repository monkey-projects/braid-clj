(ns monkey.braid.test.server-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-commons.byte-streams :as bs]
            [monkey.braid
             [server :as sut]
             [utils :as u]]
            [org.httpkit.server :as http]
            [ring.mock.request :as rm]))

(deftest start-server
  (testing "starts aleph http server"
    (with-redefs [http/run-server (constantly ::started)]
      (is (= ::started (sut/start-server {:insecure true})))))

  (testing "fails when no bot token for secure server"
    (with-redefs [http/run-server (constantly ::started)]
      (is (thrown? Exception (sut/start-server {:insecure false})))))

  (testing "passes http config to server"
    (with-redefs [http/run-server (fn [_ conf]
                                      conf)]
      (is (= {:port 12432} (sut/start-server {:insecure true
                                              :http {:port 12432}})))))

  (testing "assigns default port"
    (with-redefs [http/run-server (fn [_ conf]
                                    conf)]
      (is (= {:port 3000} (sut/start-server {:insecure true}))))))

(deftest make-handler
  (let [h (sut/make-handler {:insecure true
                             :handler (constantly nil)})
        msg-body (fn [msg]
                   (u/->transit (assoc msg :id (random-uuid))))
        request (fn [msg]
                  (-> (rm/request :put "/recv")
                      (rm/body (if (string? msg) msg (msg-body msg)))
                      (rm/header "content-type" "application/transit+json")))
        send-request (fn [msg]
                       (h (request msg)))]
    (testing "`/recv`"
      (testing " handles incoming `PUT` requests"
        (is (= 200 (-> (rm/request :put "/recv")
                       (rm/json-body {:content "test" :id (random-uuid)})
                       (h)
                       :status))))

      (testing "parses body from messagepack encoded transit"
        (is (= 200 (:status (send-request {:content "test message"})))))

      (testing "`400` when no content"
        (is (= 400 (:status (send-request {})))))

      (testing "`400` when empty content"
        (is (= 400 (:status (send-request {:content ""})))))

      (testing "security"
        (let [secured (sut/make-handler {:bot {:bot-id "test-id"
                                               :token "test-token"}
                                         :handler (constantly nil)})]
          (testing "401 when no valid header specified"
            (is (= 401 (-> (request {:content "I'm a hacker"})
                           (secured)
                           :status))))

          (testing "ok when valid header specified"
            (let [body (msg-body {:content "I'm the real deal"})
                  resp (-> (request body)
                           (rm/header "x-braid-signature" (u/generate-signature
                                                           body
                                                           "test-token"))
                           (secured))]
              (is (= 200 (:status resp)) (bs/to-string (:body resp))))))))

    (testing "404 when not found"
      (is (= 404 (-> (rm/request :ge "/nonexisting")
                     (h)
                     :status))))))

(deftest message-handler
  (testing "creates a fn"
    (is (fn? (sut/message-handler (constantly "ok")))))

  (testing "returns 200 response"
    (let [h (sut/message-handler (constantly "ok"))]
      (is (= 200 (:status (h {}))))))

  (testing "passes body to handler fn"
    (let [recv (atom [])
          h (sut/message-handler (partial swap! recv conj))
          msg {:content "test message"}]
      (is (map? (h {:parameters {:body msg}})))
      (is (= 1 (count @recv)))
      (is (= msg (first @recv))))))
