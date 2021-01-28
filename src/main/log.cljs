(ns main.log
  (:require ["fs" :as fs]))

(def config "config.edn")

(defn load-config []
  (.readFileSync fs config))
