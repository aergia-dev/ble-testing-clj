(ns renderer.ble
  (:require [renderer.ipc :as ipc]))


(defn data-sync [dev-name]
  (send-ipc {:data-sync {:dev 
