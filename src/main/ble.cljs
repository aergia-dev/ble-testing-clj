(ns main.ble
  (:require [cljs.core.async :refer [<! >! take! chan go-loop]]
            [cljs.core.async :refer-macros [go alt!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            ["node-ble" :refer [createBluetooth]]
            ;; [main.log :as log :refer [->log]]
            [main.protocol :as protocol]))
;; (def config (log/load-config))

(def config {:uuid "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
             :write-uuid "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
             :notify-uuid "6e400003-b5a3-f393-e0a9-e50e24dcca9e"})
(def filtering-name "Catmos")


;;FIX IT.
(def bt (atom nil))
(def bt-chan (chan))
(def notify (chan))

(go-loop []
  (when-let [b (<! notify)]
    (prn "notify " b)
    (prn (protocol/rsp b))
    ))

(go-loop []
  (when-let [b (<! bt-chan)]
    (reset! bt b)))


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
             :destroy destroy}))))))


(defn init []
  (go
    (let [bt (<! (create-bt))]
      (>! bt-chan bt))))


(defn device-list [resp-fn]
  (go
    (let [{:keys [bluetooth adapter destroy] :as bt} @bt]
      (prn (nil? adapter))
      (when-not (nil? adapter)
        ;; (resp-ch {:error "adapter is nil"})
        (doseq [mac (<p! (.devices adapter))]
          (prn "mac " mac)
          (-> (.waitDevice adapter mac)
              (.then (fn [dev]
                       (-> (.getName dev)
                           (.then (fn [name]
                                      (when (clojure.string/includes? name filtering-name)
                                        (resp-fn false {:cmd "device"
                                                  :contents {:name name :mac mac}}))))
                           (.catch (fn [err]  ;;can't read a name.
                                     )))))))))))


(defn battery-info []
  )


(defn connect-dev [info]
  (go
    (prn "connect dev")
    (prn "mac " (get info "mac"))
    
    (let [{:keys [bluetooth adapter destroy] :as bt} @bt
          mac (get info "mac")
          dev (<p! (.waitDevice adapter mac))]
      (prn "bring a device")
      (<p! (.disconnect dev)) ;; maybe...
      (<p! (.connect dev))
      (prn "connected in common")
      (let [gatt-server (<p! (.gatt dev))
            service (<p! (.getPrimaryService gatt-server (:uuid config)))
            w-ch (<p! (.getCharacteristic service (:write-uuid config)))
            r-ch (<p! (.getCharacteristic service (:notify-uuid config)))]

        (when (false?  (<p! (.isNotifying r-ch)))
          (<p! (.startNotifications r-ch)))

        (prn "fin in common")
        {:bluetooth bluetooth
         :adapter adapter
         :destroy destroy
         :gatt-server gatt-server
         :service service
         :dev dev
         :mac mac
         :w-ch w-ch
         :r-ch r-ch}))))

(def testmode-status (atom false))

(defn test-mode [info resp-fn]
  (prn "info " info)
  (go
  (let [bt (<! (connect-dev info))
        w-ch (:w-ch bt)
        r-ch (:r-ch bt)]
    (prn "A")
    (<p! (.writeValue w-ch (protocol/req {:cmd :normal-connection})))
    (let [read (-> (<p! (.readValue r-ch))
                   (protocol/rsp))]
      (prn "read " read)
      (when (= 0 (:res read))
        (prn "############## enter error"))
        ;; (resp-fn false {:cmd "normal-connection"
                        ;; :contents {}
                        ;; :error "normal connection fail"})))
    (prn "b")
        
    ;; (<p! (.writeValue w-ch (protocol/req {:cmd :register})))
    ;; (let [read (-> (<p! (.readValue r-ch))
    ;;                (protocol/rsp))]
    ;;   (prn "register " read))

    (.on r-ch "valuechanged" (fn [buffer]
                               (prn " ## changed " (protocol/->byte-array buffer))))
    
    (<p! (.writeValue w-ch (protocol/req {:cmd :testmode :info {:testmode-onoff info}})))
    (let [{:keys [result]} (-> (<p! (.readValue r-ch))
                               (protocol/rsp))]
      (prn "enter test mode result " result)
      (prn @testmode-status))))))

      ;; (when result
      ;;   (do
      ;;     (while @testmode-status
      ;;       (let [read (-> (<p! (.readValue r-ch))
      ;;                      (protocol/rsp-testmode))]
      ;;         (prn "read " read)
      ;;         (prn {:cmd "testmode" :contents {:mac (:mac bt)
      ;;                                                   :name (<p! (.getName (:dev bt)))
      ;;                                                   :data read}}))))
          
      ;;     (<p! (.writeValue w-ch (protocol/req {:cmd :test-mode :info {:testmode-onoff 0}}))))))))
      ;; ;; (resp-fn false {:cmd "testmode"
                     ;; :contents {}
                     ;; :error "fail to enter testmode"}))))


(defn raw-data [info resp-fn]
  (prn "info " info)
  (go
  (let [bt (<! (connect-dev info))
        w-ch (:w-ch bt)
        r-ch (:r-ch bt)]
    (.on r-ch "valuechanged" (fn [buffer]
                               (prn "changed " (protocol/->byte-array buffer))))
    
    (<p! (.writeValue w-ch (protocol/req {:cmd :raw-data-mode :info {:rawdata-onoff info}})))
    (let [{:keys [result]} (-> (<p! (.readValue r-ch))
                               (protocol/rsp))]
      (prn "enter test mode result " result)))))



(defn normal-data-sync [info resp-fn]
  (prn "in the normal data-sync")
  (go
    (let [{:keys [bluetooth adapter destroy] :as bt} @bt
          mac (get info "mac")
          dev (<p! (.waitDevice adapter mac))]
      (prn "get a dev")
      (<p! (.disconnect dev))
      (<p! (.connect dev))
      (prn "connected")
      (let [gatt-server (<p! (.gatt dev))
            service (<p! (.getPrimaryService gatt-server (:uuid config)))
            w-ch (<p! (.getCharacteristic service (:write-uuid config)))
            r-ch (<p! (.getCharacteristic service (:notify-uuid config)))]
        (prn "get a characteristic")
        (when (false?  (<p! (.isNotifying r-ch)))
          (<p! (.startNotifications r-ch)))
        
        (prn "after noti")
        (<p! (.writeValue w-ch (protocol/req {:cmd :register})))
        (let [read (-> (<p! (.readValue r-ch))
                       (protocol/rsp))]
          
          (prn "register " read))
        (<p! (.writeValue w-ch (protocol/req {:cmd :normal-connection})))
        (let [read (-> (<p! (.readValue r-ch))
                       (protocol/rsp))]
          (if (= 0 (:res read))
            (resp-fn false {:cmd "normal-connection"
                            :contents {}
                            :error "normal connection fail"})
            (<p! (.writeValue w-ch (protocol/req {:cmd :init-data-sync})))))
        
        (let [{:keys [result count]:as all} (-> (<p! (.readValue r-ch))
                                                (protocol/rsp))]
          (if (= 1 result)
            (loop [req-idx 0
                   acc []]
              (<p! (.writeValue w-ch (protocol/req {:cmd :read-data :info {:index req-idx}})))
              (let [{:keys [index timestamp activity] :as read} (-> (<p! (.readValue r-ch))
                                                                    (protocol/rsp))
                    sync-resp (-> (<p! (.readValue r-ch))
                                  (protocol/->byte-array))]

                (prn "sync resp: " sync-resp)
                (if (= req-idx count)
                  (do
                     (resp-fn true {:cmd :data-sync
                                    :contents {:mac mac
                                               :name (<p! (.getName dev))
                                               :data acc}}))
                  (recur (inc req-idx) (conj acc {:index index
                                                  :timestamp timestamp
                                                  :activity activity})))))))
        (prn "disconnect")
        (<p! (.disconnect dev))))))



(defn reset [info resp-fn]
  (prn "Reset")
  (prn (get info "mac"))
  (go
  (let [{:keys [bluetooth adapter destroy] :as bt} @bt
        mac (get info "mac")
        dev (<p! (.waitDevice adapter mac))]
    (prn "get a dev")
    (<p! (.disconnect dev))
    (<p! (.connect dev))
    (prn "connected")
    (let [gatt-server (<p! (.gatt dev))
          service (<p! (.getPrimaryService gatt-server (:uuid config)))
          w-ch (<p! (.getCharacteristic service (:write-uuid config)))
          r-ch (<p! (.getCharacteristic service (:notify-uuid config)))]
      (when (false?  (<p! (.isNotifying r-ch)))
        (<p! (.startNotifications r-ch)))
      (<p! (.writeValue w-ch (protocol/req {:cmd :reset})))
      (let [read (-> (<p! (.readValue r-ch))
                     (protocol/rsp))]
        (resp-fn false {:cmd :reset
                        :contents {:mac mac
                                   :name (<p! (.getName dev))
                                   :content read}}))))))

(defn handler [cmd info resp-fn]
  (prn "ble handler")
  (prn "cmd " cmd)
  (prn "info " info)
  (go
    (condp = cmd
      ;; "conn" (conn resp-fn)
      "scan" (device-list resp-fn)
      "data-sync" (normal-data-sync info resp-fn)
      "reset" (reset info resp-fn)
      "testmode" (do
                   (if (= true (get info "testmode-onoff"))
                     (do
                       (reset! testmode-status true)
                       (test-mode info resp-fn))
                     (reset! testmode-status false)))
      
      (>! resp-fn {:err "nothing in ble handler"}))))


;; (defn write [characteristic cmd]
;;   (prn "write " cmd)
;;   (<p! (.writeValue characteristic (protocol/req cmd))))

;; (defn read [characteristic]
;;   (let [received (-> (<p! (.readValue characteristic))
;;                      (protocol/rsp))]
;;     (prn "read " received)
;;     receivec))
