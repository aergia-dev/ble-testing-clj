(ns main.log
  (:require ["fs" :as fs]
            ["winston" :as winston]))


;; (def logger (.createLogger winston (clj->js {:level "info"
;;                                              :transport [(.Console (.transports (winston. )))
;;                                                          (.File (.transprots (winston. )) (clj->js {:filename "combined.log"}))]})))

;; (def config "config.edn")

;; (defn load-config []
;;   (.readFileSync fs config))
