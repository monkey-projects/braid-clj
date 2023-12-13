(ns user
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [monkey.braid
             [core :as c]
             [server :as s]])
  (:import java.io.PushbackReader))

(defn load-bot []
  (with-open [r (-> (io/resource "bot.edn")
                    (io/reader)
                    (PushbackReader.))]
    (edn/read r)))

(defn send-message [msg]
  @(c/send-message (load-bot) msg))

(defonce server (atom nil))

(defn stop-server []
  (swap! server (fn [srv]
                  (when srv
                    (s/stop-server srv)))))

(defn start-server [& [conf]]
  (swap! server (fn [srv]
                  (when srv
                    (s/stop-server srv))
                  (s/start-server conf))))
