(ns main.ipc
  (:require [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [main.funcs :refer [obj->clj]]
            ["node-ble" :refer [createBluetooth]]))

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
                  ;; uuid (<p! (.getUUID gatt-service))
                  ;; character-1 (<p! (.getCharacteristic gatt-service ch-uuid1))
                  ;; character-2 (<p! (.getCharacteristic gatt-service ch-uuid2))]
              ;; (prn "uuid" uuid)
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

                ;; (js/setTimeout #(go (let [r (<p! (.readValue r-ch))]
                ;;                   (prn r))) 2000)
                
                ;; (let [rr  (<p! (.readValue r-ch))]
                ;;   (prn rr)
                ;;   (print "read" (.from js/Uint8Array rr)))

                ;; (prn "!!" (js/Uint8Array. (get cmd :test-mode)))
                ;; (let [wv (.from js/Buffer (js/Uint8Array. (get cmd :test-mode)))];;(.from js/Buffer (js/ArrayBuffer. (get cmd :register)))]
                ;;   (prn "write value " wv)
                ;;   (prn "write value " (.values wv))
                ;;   (<p! (.writeValue ch wv )))
                
                ;; (let [r (<p! (.readValue ch))]
                ;;   (prn "read buffer" r)
                ;;   (prn "read msg" (.toString r)))

              ;; (.log js/console (str "read"(.toString (<p! (.readValue ch)))))
              
              ;; (<p! (.disconnect dev))
              ;; (destroy)
              ))))

            (catch js/Object e (prn "error" e))))))
          ;; (finally 
            ;; (<p! (.disconnect dev))
            ;; (destroy))))))


(defn receive-ipc [event args]
  (prn "received ipc" args)
  (ble-test)
  (.reply event "fromMain" "well"))
