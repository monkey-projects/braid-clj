(ns monkey.braid.build
  (:require [monkey.ci.build
             [api :as api]
             [core :as c]
             [shell :as s]]))

(def unit-test
  (s/bash "clj -X:junit:test"))

(defn deploy [ctx]
  (s/bash "clj -X:jar:deploy"
          {:extra-env (-> ctx
                          (api/build-params)
                          (select-keys ["CLOJARS_USERNAME" "CLOJARS_PASSWORD"]))}))

(c/defpipeline publish
  [unit-test
   deploy])

[publish]
