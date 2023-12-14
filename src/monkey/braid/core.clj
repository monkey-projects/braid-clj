(ns monkey.braid.core
  (:require [clojure.spec.alpha :as spec]
            [clojure.tools.logging :as log]
            [config.core :as cc]
            [medley.core :as mc]
            [monkey.braid
             [server :as s]
             [utils :as u]]
            [org.httpkit.client :as http]))

(def default-url "https://braid.chat/api")

(defn make-bot
  "Creates a bot structure using the config"
  [config]
  (merge {:url default-url} config))

(def bot->auth (juxt :bot-id :token))

(defn send-message
  "Sends a message as given bot.  The `:content` and `:thread-id` must be specified.
   Returns a deferred, so sending is async."
  [{:keys [url] :as bot} msg]
  (http/post (str url "/bots/message")
             {:body (->> msg
                         (merge {:id (random-uuid)
                                 :mentioned-user-ids []
                                 :mentioned-tag-ids []})
                         (u/->transit))
              :headers {:content-type "application/transit+json"}
              :basic-auth (bot->auth bot)}))

(def default-config
  {:http {:port 3000}
   :insecure false
   :bot {:url default-url}})

(defn env->config
  "Creates a configuration map from the environment values."
  ([env]
   (letfn [(add-bot-info [r]
             (update r :bot
                     mc/assoc-some
                     :bot-id (:braid-bot-id env)
                     :token (:braid-bot-token env)
                     :url (:braid-url env)))
           (add-http-info [r]
             (update r :http mc/assoc-some :port (:http-port env)))]
     (-> default-config
         (add-bot-info)
         (add-http-info))))
  ([]
   (env->config cc/env)))

(defn send-response
  "If `msg` is a valid message, sends it to the configured Braid server."
  [conf msg]
  (when (spec/valid? ::s/msg-out msg)
    (log/debug "Sending message out:" msg)
    (send-message (:bot conf) msg)))

(defn start-bot-server [conf handler]
  (s/start-server (assoc conf :handler (comp (partial send-response conf) handler))))

(defn stop-bot-server [srv]
  (s/stop-server srv))
