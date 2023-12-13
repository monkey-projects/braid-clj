(ns user
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [monkey.braid
             [core :as c]
             [server :as s]
             [utils :as u]])
  (:import java.io.PushbackReader))

(defn load-bot []
  (with-open [r (-> (io/resource "bot.edn")
                    (io/reader)
                    (PushbackReader.))]
    (edn/read r)))

(defn send-message [msg]
  @(c/send-message (load-bot) msg))

(defn handle-message
  "Default handler"
  [{msg :message}]
  (log/info "Message content:" (:content msg)))

(defonce server (atom nil))

(defn stop-server []
  (swap! server (fn [srv]
                  (when srv
                    (s/stop-server srv)))))

(defn start-server [& [conf]]
  (swap! server (fn [srv]
                  (when srv
                    (s/stop-server srv))
                  (s/start-server (merge {:insecure true} conf)))))

(defn secure-server [token]
  (start-server {:bot {:bot-id (random-uuid)
                       :token token}
                 :insecure false}))
