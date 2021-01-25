(ns main.ipc
  (:require [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [main.funcs :refer [obj->clj]]
            ["node-ble" :refer [createBluetooth]]))

(def m1 "CF:A1:CB:20:79:18")
(def uuid-1 "00001801-0000-1000-8000-00805f9b34fb")
(def uuid-2 "6e400001-b5a3-f393-e0a9-e50e24dcca9e")

(defn ble-test []
  (go
    (let [bt (js->clj (createBluetooth))
          bluetooth (get bt "bluetooth")
          destroy (get bt "destroy")
          adapter (-> (.defaultAdapter bluetooth)
                      (<p!))]
      (try
        (<p! (.startDiscovery adapter))
        (prn (<p! (.devices adapter)))
        (prn (<p! (.toString adapter)))
        (let [dev (<p! (.waitDevice adapter m1))]
          (prn "dev name" (<p! (.getName dev)))
          (<p! (.connect dev))
          (prn "connected")
          (let [gatt-server (<p! (.gatt dev))
                services (<p! (.services gatt))]
            (prn "services" services)
            (prn "gatt " gatt-server)
            (let [gatt-service (<p! (.getPrimaryService gatt-server uuid-1))]
                  ;;6e400001-b5a3-f393-e0a9-e50e24dcca9e
                  ;; c (<p! (.getCharacteristic s uuid-1))]
              ;; (prn s))
              (prn "uuid" (<p! (.getUUID gatt-service)))
              (prn (<p! (.characteristics gatt-service)))
              )))
            ;; (prn (<p! (.getPrimaryService gatt (first services))))

            (catch js/Object e (prn "error" e)))
          (finally 
            (<p! (.disconnect dev))
            (destroy)))))


(defn receive-ipc [event args]
  (prn "received ipc" args)
  (ble-test)
  (.reply event "fromMain" "well"))
