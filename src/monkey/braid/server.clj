(ns monkey.braid.server
  "Code for the bot http server"
  (:require [aleph.http :as http]
            [buddy.core
             [codecs :as codecs]
             [mac :as mac]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [medley.core :refer [update-existing]]
            [muuntaja.core :as mc]
            [reitit.coercion.spec :as rcs]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware
             [exception :as rrme]
             [muuntaja :as rrmm]
             [parameters :as rrmp]]
            [ring.util.response :as rur]))

(s/def ::content (every-pred string? not-empty))
(s/def ::id uuid?)
(s/def ::thread-id uuid?)
(s/def ::group-id uuid?)
(s/def ::user-id uuid?)
(s/def ::created-at string?)
(s/def ::mentioned-user-ids (s/coll-of uuid?))
(s/def ::mentioned-tag-ids (s/coll-of uuid?))

(s/def ::msg (s/keys :req-un [::content ::id]
                     :opt-un [::thread-id ::group-id ::user-id
                              ::created-at ::mentioned-user-ids ::mentioned-tag-ids]))

(defn recv-message [req]
  (let [body (get-in req [:parameters :body])]
    (log/debug "Got incoming message:" body)
    (rur/response "ok")))

(defn- valid-security-header? [req conf]
  (let [token (get-in conf [:bot :token])
        h (get-in req [:headers "x-braid-signature"])]
    (and h
         (mac/verify (:body req)
                     (codecs/hex->bytes h)
                     {:key token
                      :alg :hmac+sha256}))))

(defn- validate-security-header [handler {:keys [insecure] :as conf}]
  (if (and (not insecure) (nil? (get-in conf [:bot :token])))
    (throw (ex-info "Bot token must be specified for secure configuration" conf)))
  (fn [req]
    (if (or insecure (valid-security-header? req conf))
      (handler req)
      (-> (rur/response "Not authorized")
          (rur/status 401)))))

(defn- stringify-body
  "Since the raw body could be read more than once (security, content negotation...),
   this interceptor replaces it with a string that can be read multiple times.  This
   should only be used for requests that have reasonably small bodies!  In other
   cases, the body could be written to a temp file."
  [h]
  (fn [req]
    (-> req
        (update-existing :body (fn [s]
                                 (when (instance? java.io.InputStream s)
                                   (slurp s))))
        (h))))

(defn- trace-req [h]
  (fn [req]
    (log/trace "Incoming request:" req)
    (h req)))

(defn make-router [conf]
  (ring/router
   [["/recv" {:put {:handler recv-message
                    :parameters {:body ::msg}}
              :middleware [[validate-security-header conf]]}]]
   {:data {:muuntaja mc/instance
           :coercion rcs/coercion
           :middleware [rrme/exception-middleware
                        stringify-body
                        trace-req
                        rrmm/format-middleware
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
