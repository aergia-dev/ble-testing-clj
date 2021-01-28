(ns main.ble
  (:require [cljs.core.async :refer [<! >! take!]]
            [cljs.core.async :refer-macros [go alt!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            
            ["node-ble" :refer [createBluetooth]]
            [main.log :as log]))

;; (def uuid   "6e400001-b5a3-f393-e0a9-e50e24dcca9e")
;; (def uuid-write "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
;; (def uuid-notity "6e400003-b5a3-f393-e0a9-e50e24dcca9e")

;; (def config (log/load-config))

(def config {:uuid "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
             :write-uuid "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
             :notify-uuid "6e400003-b5a3-f393-e0a9-e50e24dcca9e"})

  
(defn create-bt []
  (go
    (let [bt (js->clj (createBluetooth))
          bluetooth (get bt "bluetooth")
          destroy (get bt "destroy")
          adapter (<p! (.defaultAdapter bluetooth))]
      (try
        (if (<p! (.isDiscovering adapter))
          {:error "already started"}
          (do
            (<p! (.startDiscovery adapter))
            {:bluetooth bluetooth
             :adapter adapter
             :destroy destroy}))
        (catch js/Object e (prn "catched in create-bt " e))))))



(defn device-list [resp-fn]
  (go
    (let [{:keys [bluetooth adapter destroy] :as bt} (<! (create-bt))]
      (if (nil? adapter)
        (>! resp-ch {:error "adapter is nil"})
        (do
          (doseq [mac (<p! (.devices adapter))]
            (prn "mac " mac)
            (-> (.waitDevice adapter mac)
                (.then (fn [dev]
                         (-> (.getName dev)
                             (.then (fn [name]
                                      (go ;;(>! resp-ch {:name name :mac mac})
                                        (resp-fn (clj->js {:dev {:name name :mac mac}}))
                                        ;; (prn "then" name)
                                        (<p! (.disconnect dev)))))
                             (.catch (fn [err]
                                       (go ;;(>! resp-ch {:name nil :mac mac})
                                         (resp-fn (clj->js {:dev {:name nil :mac mac}}))
                                         ;; (prn "catch" err)
                                         (<p! (.disconnect dev)))))))))))))))


(defn battery-info []
  )


(defn normal-data-sync []
  )


(defn test-mode []
  )



(defn handler [msg resp-fn]
  (prn "ble handler")
  (prn "msg " msg)
  ;; (prn "ch " ch)
   ;; (prn "1"   (device-list resp-ch)
      ;; (.then (fn [d]
  ;; (prn "A" d))))
  (go
    (condp = msg
      "scan" (device-list resp-fn)
      (>! resp-ch {:err "nothing in ble handler"}))))
  

               
