(ns monkey.braid.utils
  (:require [buddy.core
             [mac :as mac]
             [codecs :as codecs]]
            [muuntaja.core :as mc]))

(def ->transit (comp slurp (mc/encoder mc/instance "application/transit+json")))

(defn generate-signature
  "Generates a signature header for the given body with the specified token"
  [body token]
  (-> (mac/hash body {:key token
                      :alg :hmac+sha256})
      (codecs/bytes->hex)))
