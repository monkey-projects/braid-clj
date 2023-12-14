(ns monkey.braid.build
  (:require [clojure.string :as cs]
            [monkey.ci.build
             [api :as api]
             [core :as c]
             [shell :as s]]))

(defn clj-container [name & args]
  "Executes script in clojure container"
  {:name name
   :container/image "docker.io/clojure:temurin-21-tools-deps-alpine"
   :script [(cs/join " " (concat ["clojure"] args))]})

(def unit-test
  (clj-container "unit-test" "-X:junit:test"))

(defn deploy [ctx]
  (-> (clj-container "deploy" "-X:jar:deploy")
      (assoc :container/env (-> ctx
                                (api/build-params)
                                (select-keys ["CLOJARS_USERNAME" "CLOJARS_PASSWORD"])))))

(c/defpipeline publish
  [unit-test
   deploy])

[publish]
