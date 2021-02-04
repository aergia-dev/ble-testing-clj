(ns main.ipc
  (:require [cljs.core.async :refer [chan]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [main.funcs :refer [obj->clj]]
            [main.ble :as ble]
            [main.fs :refer [->log]]
            ["fs" :as fs]
            ["node-ble" :refer [createBluetooth]])
  (:require-macros [cljs.core.async.macros :as m :refer [go go-loop]]))

(def m1 "CF:A1:CB:20:79:18")
(def m2 "C4:C3:1E:A4:75:1D")

;; (def uuid     "6e40000a-b5a3-f393-e0a9-e50e24dca9e")
(def uuid-1   "6e400001-b5a3-f393-e0a9-e50e24dcca9e")
(def ch-uuid1 "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
(def ch-uuid2 "6e400003-b5a3-f393-e0a9-e50e24dcca9e")

(def cmd {:register [0xa0 0x08 0x30 0x30 0x30 0x30 10 20 30 40]
          :normal-connection [0xc0 0x0b 0x30 0x30 0x30 0x30 1 2 3 4]
          :sync-data [0xc1 0x04 0x30 0x30 0x30 0x30]
          :test-mode [0xe0 0x05 0x30 0x30 0x30 0x30 0x1]})


(defn ble-test []
  (go
    (let [bt (js->clj (createBluetooth))
          bluetooth (get bt "bluetooth")
          destroy (get bt "destroy")
          adapter (<p! (.defaultAdapter bluetooth))]
      (try
        (<p! (.startDiscovery adapter))
        (prn (<p! (.devices adapter)))
        (prn (<p! (.toString adapter)))
        (let [dev (<p! (.waitDevice adapter m2))]
          (prn "dev name" (<p! (.getName dev)))
          (prn "dev addr" (<p! (.getAddress dev)))
          (<p! (.connect dev))
          (prn "connected")
          (let [gatt-server (<p! (.gatt dev))]
            (prn "gatt server" gatt-server)
            (prn "services " (<p! (.services gatt-server)))
            (let [service (<p! (.getPrimaryService gatt-server uuid-1))]
              (prn "gatt service")
              (prn "chss." (<p! (.characteristics service)))
              (let [w-ch (<p! (.getCharacteristic service ch-uuid1))
                    r-ch (<p! (.getCharacteristic service ch-uuid2))]

                (<p! (.startNotifications r-ch))
                (.on r-ch "valuechanged" (fn [buffer]
                                           (prn (.from js/Uint8Array buffer))))
                
                (prn "timestamp" (.now js/Date))
                ;;registration
                
                (let [ww  (.from js/Buffer (js/Uint8Array. (get cmd :register)))]
                  (prn "write" (.from js/Uint8Array ww)))
                
                (<p! (.writeValue w-ch (.from js/Buffer (js/Uint8Array. (get cmd :register)))))
              
              ;; (<p! (.disconnect dev))
              ;; (destroy)
              ))))

            (catch js/Object e (prn "error" e))))))
          ;; (finally 
            ;; (<p! (.disconnect dev))
            ;; (destroy))))))

(def resp-ch (chan))

(defn receive-ipc [event args]
  (let [all (-> args js->clj)
        type (get all "type")
        cmd (get all "cmd")
        info (get all "info")
        resp-fn (fn [with-log data]
                  (let [{:keys [name normal-data]} (:contents data)]
                    (doseq [data normal-data]
                      (println data)))
                  ;; (prn "with log" with-log)
                  ;; (when with-log
                    ;; (->log data))
                  (.reply event "fromMain" (clj->js data)))]
    (prn "receive-ipc" all)
    (condp = type
      "bt" (ble/handler cmd info ->log resp-fn )
      {:error "none"})))


;;info format
;;normal connection {:info {:mac "12:23:34:45"}}
;;test mode {:info {:testmode-onoff "1" or "0"}}

;;resf-fn
;;(resp-fn false {:cmd "normal-connection"
;;                :contents {}
;;                :error "normal connection fail"})
