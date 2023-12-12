(ns user
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [monkey.braid.core :as c])
  (:import java.io.PushbackReader))

(defn load-bot []
  (with-open [r (-> (io/resource "bot.edn")
                    (io/reader)
                    (PushbackReader.))]
    (edn/read r)))

(defn send-message [msg]
  @(c/send-message (load-bot) msg))
