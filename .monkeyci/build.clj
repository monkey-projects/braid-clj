(ns monkey.braid.build
  (:require [monkey.ci.build
             [core :as c]
             [shell :as s]]))

(c/defpipeline unit-test
  [(s/bash "clj -X:junit:test")])

(c/defpipeline uberjar
  [(s/bash "clj -X:uber")])

(c/defpipeline build-image
  [(s/bash "podman build -t braid-bot .")])

[unit-test
 uberjar]
