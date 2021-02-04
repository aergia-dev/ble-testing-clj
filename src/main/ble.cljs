(ns main.ble
  (:require [cljs.core.async :refer [<! >! take! chan go-loop]]
            [cljs.core.async :refer-macros [go alt!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            ["node-ble" :refer [createBluetooth]]
            [kitchen-async.promise :as p]
            [main.protocol :as protocol]))

;; (def config (log/load-config))

(def config {:uuid "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
             :write-uuid "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
             :notify-uuid "6e400003-b5a3-f393-e0a9-e50e24dcca9e"})
(def filtering-name "Catmos")


;;FIX IT.
(def bt (atom nil))
(def bt-chan (chan))


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
        (do
          
          {:bluetooth bluetooth
           :adapter adapter
           :destroy destroy})))))


(defn init []
  (go
    (let [bt (<! (create-bt))]
      (>! bt-chan bt))))


(defn device-list [resp-fn]
  (go
    (let [{:keys [bluetooth adapter destroy] :as bt} @bt]
      (when (<p! (.isDiscovering adapter))
        (<p! (.stopDiscovery adapter)))
      (<p! (.startDiscovery adapter))

      (when-not (nil? adapter)
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
    (prn "try to connect " (get info "mac"))
    (try
      (let [{:keys [bluetooth adapter destroy] :as bt} @bt
            mac (get info "mac")
            dev (<p! (.waitDevice adapter mac))]
        (prn "bring a device")
        ;; (when (<p! (.isConnected dev)) ;; maybe...
        ;;   (<p! (.disconnect dev)))
        ;; (<p! (.connect dev))
        
        (when-not (<p! (.isConnected dev))
          (<p! (.connect dev)))
          
        (prn "connected in common")
        (let [gatt-server (<p! (.gatt dev))
              service (<p! (.getPrimaryService gatt-server (:uuid config)))
              dev-name (<p! (.getName dev))
              w-ch (<p! (.getCharacteristic service (:write-uuid config)))
              r-ch (<p! (.getCharacteristic service (:notify-uuid config)))]
          (prn "fin in common connection")
          {:bluetooth bluetooth ;;not using
           :adapter adapter ;;not using
           :destroy destroy ;;not using
           :gatt-server gatt-server ;;not using
           :service service ;;note using
           :dev dev ;;note using
           :dev-name dev-name
           :mac mac
           :w-ch w-ch
           :r-ch r-ch}))
      (catch js/Error err {:error {:mac mac
                                   :name dev-name
                                   :contents (str "connection error - " (js->clj err))}}))))

(def dum (atom nil))
(defn rawmode [info ->log resp-fn]
  (prn "in the raw data")
  (go
    (let [{:keys [w-ch r-ch dev] :as bt} (<! (connect-dev info))
          rawmode-onoff (get info "raw-onoff")]
      (prn "raw mode: " rawmode-onoff)
      (if (= 1 rawmode-onoff)
        (do
          (prn "start a rawmode")

      

          (.on r-ch "valuechanged" (fn [buffer]
                                     (prn (protocol/rsp-rawmode buffer))))
     (when (true? (<p! (.isNotifying r-ch)))
            (<p! (.stopNotifications r-ch)))
          (<p! (.startNotifications r-ch))                                     
                                     ;; (->log {:type :rawmode-data
                                     ;; :name dev-name
                                     ;; :mac mac
                                     ;; :contents read})
          ;; (prn "changed " (protocol/->byte-array buffer))))
          (prn "###" (.listeners r-ch "valuechanged"))
          (reset! dum r-ch)
          (<p! (.writeValue w-ch (protocol/req {:cmd :raw-data-mode :info {:rawmode-onoff 1}}))))
        (do
          (prn "stop a rawmode")
          (when (true? (<p! (.isNotifying r-ch)))
            (<p! (.stopNotifications r-ch)))


          (prn "###" (.listeners @dum "valuechanged"))
          
          (.removeAllListeners @dum)
          
          (prn "###" (.listeners r-ch "valuechanged"))
          
          (prn "a")
          (<p! (.writeValue w-ch (protocol/req {:cmd :raw-data-mode :info {:rawmode-onoff 0}})))
          (prn "b")
          (<p! (.disconnect dev))
          (prn "C"))))))
          ;; (resp-fn false {:cmd :rawmode-onoff
                          ;; :contents {}
                          ;; :error nil}))))))
  
;; (defn start-notification [r-ch]
;;   (go
;;     (when (false? (<p! (.isNotifying r-ch)))
;;       (<p! (.startNotifications r-ch)))))

;; (defn write [characteristic cmd]
;;   (go                                
;;     (<p! (.writeValue characteristic (protocol/req {:cmd cmd})))))

;; (defn read-eval [characteristic op eval-k eval-v]
;;   (go
;;     (let [read (-> (<p! (.readValue characteristic))
;;                    (protocol/rsp))]
;;       (prn "READ" read)
;;       (prn eval-k (get read eval-k) eval-v)
;;       (if-not (op (get read eval-k) eval-v)
;;         (throw {:err (str "read error: " eval-k)})
;;         true))))


;; (defn wr [w-ch r-ch cmd op eval-k eval-v resp-fn]
;;   (let [rsp-err (do
;;                   (prn "excute faile: " cmd)
;;                   (resp-fn false {:cmd :data-sync
;;                                   :contents {}
;;                                   :error (str "cmd fail: " cmd)}))]
;;     (p/try (.writeValue characteristic (protocol/req {:cmd cmd}))
;;            (p/catch js/Error e (rsp-err)))

;;     (p/try (let [read (-> (<p! (.readValue characteristic))
;;                           (protocol/rsp))]
;;              (prn "READ" read)
;;              (prn eval-k (get read eval-k) eval-v)
;;              (if-not (op (get read eval-k) eval-v)
;;                (rsp-err)
;;                true))
;;            (p/catch js/Error e (resp-fn)))))




;; (defn normal-data-sync [info ->log resp-fn]
;;   (prn "in the normal data-sync")
;;   (prn "info " info)
;;   (go
;;     (let [{:keys [w-ch r-ch dev mac dev-name] :as bt} (<! (connect-dev info))]
;;       (try      
;;         (start-notification r-ch)

;;         ;; (write w-ch :register)
;;         ;; (prn (<! (read-eval r-ch not= :mac [0 0 0 0])))
;;         (wr w-ch r-ch :register not= :mac [0 0 0 0] resp-fn)
;;         (wr w-ch r-ch :normal-connection = :result 1 resp-fn)



;;         ;; (write w-ch :normal-connection)
;;         ;; (prn "2")
;;         ;; (if (<! (read-eval r-ch = :result 1))
;;         ;;   (resp-fn false {:cmd :data-sync
;;         ;;                   :contents {}
;;         ;;                   :error "normal connection fail"})
;;         ;;   (prn "a")
;;         ;;   (<p! (.writeValue w-ch (protocol/req {:cmd :init-data-sync}))))

;;         ;; (prn "b")

;;         (let [{:keys [result count]:as all} (-> (<p! (.readValue r-ch))
;;                                                   (protocol/rsp))]
;;           (if (= 1 result)
;;             (loop [req-idx 0
;;                    acc []]
;;               (<p! (.writeValue w-ch (protocol/req {:cmd :read-data :info {:index req-idx}})))
;;               (let [{:keys [index timestamp activity] :as read} (-> (<p! (.readValue r-ch))
;;                                                                     (protocol/rsp))
;;                     sync-resp (-> (<p! (.readValue r-ch))
;;                                   (protocol/->byte-array))]

;;                 (if (> req-idx count)
;;                   (do
;;                     ;;responde of data sync - once
;;                     (resp-fn true {:cmd :data-sync
;;                                    :contents {:mac mac
;;                                               :name dev-name 
;;                                               :normal-data acc}}))
;;                   (do
;;                     (->log {:type :normal-data
;;                             :name dev-name
;;                             :mac mac
;;                             :contents read})

;;                     (recur (inc req-idx) (conj acc {:index index
;;                                                     :timestamp timestamp
;;                                                     :activity activity}))))))
;;             (resp-fn false {:cmd :data-sync
;;                             :contents {:mac mac
;;                                        :name dev-name
;;                                        :err "init data sync err"}})))

;;         ;; (<p! (.disconnect dev))
;;         (catch js/Error err (do
;;                               (prn "error in normal data sync")
;;                               (prn (js->clj err))))
;;         (finally
;;           (do (prn "finalized normal data sync")
;;               (.disconnect dev)))))))


(defn normal-data-sync [info ->log resp-fn]
  (prn "in the normal data-sync")
  (go
    (try
      (let [{:keys [bluetooth adapter destroy] :as bt} @bt
            mac (get info "mac")
            dev (<p! (.waitDevice adapter mac))]
        (prn "get a dev")
        
        (when (<p! (.isConnected dev)) ;; maybe...
          (<p! (.disconnect dev)))
        (<p! (.connect dev))
        
        (let [gatt-server (<p! (.gatt dev))
              service (<p! (.getPrimaryService gatt-server (:uuid config)))
              w-ch (<p! (.getCharacteristic service (:write-uuid config)))
              r-ch (<p! (.getCharacteristic service (:notify-uuid config)))
              dev-name (<p! (.getName dev))]
          
          (when (false?  (<p! (.isNotifying r-ch)))
            (<p! (.startNotifications r-ch)))
          
          (<p! (.writeValue w-ch (protocol/req {:cmd :register})))
          (let [read (-> (<p! (.readValue r-ch))
                         (protocol/rsp))]

            
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
                                                                        (protocol/rsp))]
                        ;; sync-resp (-> (<p! (.readValue r-ch))
                                      ;; (protocol/->byte-array))]
                    ;; (prn "sync resp: " sync-resp)
                    (if (= req-idx count)
                      (do
                        ;;responde of data sync - once
                        (resp-fn true {:cmd :data-sync
                                       :contents {:mac mac
                                                  :name dev-name 
                                                  :normal-data acc}}))
                      (do
                        (->log {:type :normal-data
                                :name dev-name
                                :mac mac
                                :contents read})
                        
                        (recur (inc req-idx) (conj acc {:index index
                                                        :timestamp timestamp
                                                        :activity activity}))))))))
            (<p! (.stopNotifications r-ch))
            
            (prn "disconnect")
            (<p! (.disconnect dev)))))
      (catch js/Error err (prn "error in normal data sync" err)))))



(defn reset [info resp-fn]
  (prn "Reset")
  (prn (get info "mac"))
  (go
    (let [{:keys [bluetooth adapter destroy] :as bt} @bt
          mac (get info "mac")
          dev (<p! (.waitDevice adapter mac))]
      (prn "get a dev")
      
      (when (<p! (.isConnected dev)) 
        (<p! (.disconnect dev)))
      (<p! (.connect dev))
      
      (prn "connected")
      (let [gatt-server (<p! (.gatt dev))
            service (<p! (.getPrimaryService gatt-server (:uuid config)))
            w-ch (<p! (.getCharacteristic service (:write-uuid config)))
            r-ch (<p! (.getCharacteristic service (:notify-uuid config)))]
        (when (false?  (<p! (.isNotifying r-ch)))
          (<p! (.startNotifications r-ch)))

        (.on r-ch "valuechanged" (fn [buffer]
                                   (prn "changed " (protocol/->byte-array buffer))
                                   (protocol/rsp buffer)))
        

        (<p! (.writeValue w-ch (protocol/req {:cmd :reset})))
        (let [read (-> (<p! (.readValue r-ch))
                       (protocol/rsp))]
          (resp-fn false {:cmd :reset
                          :contents {:mac mac
                                     :name (<p! (.getName dev))
                                     :content read}})))
      (.disconnect dev))))

(defn handler [cmd info ->log resp-fn]
  (prn "ble handler")
  (prn "cmd " cmd)
  (prn "info " info)
  (go
    (condp = cmd
      ;; "conn" (conn resp-fn)
      "scan" (device-list resp-fn)
      "data-sync" (normal-data-sync info ->log resp-fn)
      "reset" (reset info resp-fn)
      "testmode" (do
                   (if (= 1 (get info "testmode-onoff"))
                     (do
                       (reset! testmode-status true)
                       (test-mode info resp-fn))
                     (reset! testmode-status false)))
      "rawmode" (rawmode info resp-fn)
                
      (>! resp-fn {:err "nothing in ble handler"}))))


;; (defn write [characteristic cmd]
;;   (prn "write " cmd)
;;   (<p! (.writeValue characteristic (protocol/req cmd))))

;; (defn read [characteristic]
;;   (let [received (-> (<p! (.readValue characteristic))
;;                      (protocol/rsp))]
;;     (prn "read " received)
;;     receivec))

(def testmode-status (atom false))

(defn test-mode [info resp-fn]
  (go
    (prn "test-mode func")
    (let [{:keys [bluetooth adapter destroy] :as bt} @bt
          mac (get info "mac")
          dev (<p! (.waitDevice adapter mac))]
      (prn "get a dev")
      ;; (<p! (.disconnect dev))
      (<p! (.connect dev))
      (prn "connected")
      (let [gatt-server (<p! (.gatt dev))
            service (<p! (.getPrimaryService gatt-server (:uuid config)))
            w-ch (<p! (.getCharacteristic service (:write-uuid config)))
            r-ch (<p! (.getCharacteristic service (:notify-uuid config)))]
        (prn "get a characteristic")
        (when (true?  (<p! (.isNotifying r-ch)))
          (<p! (.stopNotifications r-ch))
          (<p! (.startNotifications r-ch)))
        
        
        ;; (<p! (.writeValue w-ch (protocol/req {:cmd :register})))
        ;; (let [read (-> (<p! (.readValue r-ch))
        ;;                (protocol/rsp))]
        ;;   (prn "register " read))

        (.on r-ch "valuechanged" (fn [buffer]
                                   (prn " ## changed " (protocol/->byte-array buffer))))

        (prn "testmode in" (protocol/req {:cmd :testmode :info {:testmode-onoff (get info "testmode-onoff")}}))
        (<p! (.writeValue w-ch (protocol/req {:cmd :testmode :info {:testmode-onoff (get info "testmode-onoff")}})))
        ;; (let [{:keys [result]} (-> (<p! (.readValue r-ch))
        ;;                            (protocol/rsp))]
        ;;   (prn "enter test mode result " result))

        ;; (when result
        ;;   (do
        (while @testmode-status
          )))))
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

