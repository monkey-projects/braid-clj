(ns monkey.braid.test.server-test
  (:require [clojure.test :refer [deftest testing is]]
            [aleph.http :as http]
            [monkey.braid
             [server :as sut]
             [utils :as u]]
            [ring.mock.request :as rm]))

(deftest start-server
  (testing "starts aleph http server"
    (with-redefs [http/start-server (constantly ::started)]
      (is (= ::started (sut/start-server {})))))

  (testing "passes http config to server"
    (with-redefs [http/start-server (fn [_ conf]
                                      conf)]
      (is (= {:port 12432} (sut/start-server {:http {:port 12432}})))))

  (testing "assigns default port"
    (with-redefs [http/start-server (fn [_ conf]
                                      conf)]
      (is (= {:port 3000} (sut/start-server {}))))))

(deftest make-handler
  (let [h (sut/make-handler {})]
    (testing "`/recv`"
      (testing " handles incoming `PUT` requests"
        (is (= 200 (-> (rm/request :put "/recv")
                       (rm/json-body {:content "test"})
                       (h)
                       :status))))

      (testing "parses body from messagepack encoded transit"
        (let [body (u/->transit {:content "test message"})]
        (is (= 200 (-> (rm/request :put "/recv")
                       (rm/body body)
                       (rm/header "content-type" "application/transit+json")
                       (h)
                       :status))))))

    (testing "404 when not found"
      (is (= 404 (-> (rm/request :ge "/nonexisting")
                     (h)
                     :status))))))
