(ns monkey.braid.server
  "Code for the bot http server"
  (:require [aleph.http :as http]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [muuntaja.core :as mc]
            [reitit.coercion.spec :as rcs]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware
             [muuntaja :as rrmm]
             [parameters :as rrmp]]
            [ring.util.response :as rur]))

(s/def ::content (every-pred string? not-empty))
(s/def ::msg (s/keys :req-un [::content]))

(defn recv-message [req]
  (let [body (get-in req [:parameters :body])]
    (log/debug "Got incoming message:" body)
    (rur/response "ok")))

(defn make-router [conf]
  (ring/router
   [["/recv" {:put {:handler recv-message
                    :parameters {:body ::msg}}}]]
   {:data {:muuntaja mc/instance
           :coercion rcs/coercion
           :middleware [rrmm/format-middleware
                        rrc/coerce-exceptions-middleware
                        rrc/coerce-request-middleware]}}))

(defn make-handler [conf]
  (ring/ring-handler
   (make-router conf)
   (ring/routes
    (ring/redirect-trailing-slash-handler)
    (ring/create-default-handler))))

(defn start-server [conf]
  (http/start-server
   (make-handler conf)
   (merge {:port 3000} (:http conf))))

(defn stop-server [s]
  (.close s))
