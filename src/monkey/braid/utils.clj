(ns monkey.braid.utils
  (:require [muuntaja.core :as mc]))

(def ->transit (comp slurp (mc/encoder mc/instance "application/transit+json")))
