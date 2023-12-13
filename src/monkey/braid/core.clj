(ns monkey.braid.core
  (:require [aleph.http :as http]
            [monkey.braid.utils :as u]))

(def default-url "https://braid.chat/api")

(defn make-bot
  "Creates a bot structure using the config"
  [config]
  (merge {:url default-url} config))

(def bot->auth (juxt :bot-id :token))

(defn send-message
  "Sends a message as given bot.  The `:content` and `:thread-id` must be specified."
  [{:keys [url] :as bot} msg]
  (http/post (str url "/bots/message")
             {:body (->> msg
                         (merge {:id (random-uuid)
                                 :mentioned-user-ids []
                                 :mentioned-tag-ids []})
                         (u/->transit))
              :headers {:content-type "application/transit+json"}
              :basic-auth (bot->auth bot)}))
