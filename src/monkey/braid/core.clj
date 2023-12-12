(ns monkey.braid.core
  (:require [aleph.http :as http]
            [cognitect.transit :as ct])
  (:import java.io.ByteArrayOutputStream))

(def default-url "https://braid.chat/api")

(defn make-bot
  "Creates a bot structure using the config"
  [config]
  (merge {:url default-url} config))

(defn ->transit [x]
  (let [out (ByteArrayOutputStream.)
        w (ct/writer out :json)]
    (ct/write w x)
    (.toString out)))

(def bot->auth (juxt :bot-id :token))

(defn send-message
  "Sends a message as given bot.  The `:content` and `:thread-id` must be specified."
  [{:keys [url] :as bot} msg]
  (http/post (str url "/bots/message")
             {:body (->> msg
                         (merge {:id (random-uuid)
                                 :mentioned-user-ids []
                                 :mentioned-tag-ids []})
                         (->transit))
              :headers {:content-type "application/transit+json"}
              :basic-auth (bot->auth bot)}))
