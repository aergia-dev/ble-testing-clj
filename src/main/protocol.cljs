(ns main.protocol
  (:require [main.funcs :as f]))

(def keyword-sz {:timestamp {:size 4}
                 :password {:size 4
                            :type :raw}
                 :index {:size 2}
                 :testmode-onoff {:size 1}
                 :rawmode-onoff {:size 1}
                 :result {:size 1}
                 :count {:size 2}
                 :activity {:size 2}
                 :mac {:size 6
                       :type :raw}
                 :serial {:size 8
                          :type :raw}
                 :x {:size 4
                     :type :raw}
                 :y {:size 4
                     :type :raw}
                 :z {:size 4
                     :type :raw}})

(def protocol {:register {:req {:cmd [0xA0 0x08]
                                :sub [:password :timestamp]}
                          :rsp {:cmd [0xB0 0x12]
                                :sub [:password :mac :serial]}}
               :de-register {:req {:cmd [0xA1 0x05]
                                   :sub [:password 0x01]}
                             :rsp {:cmd [0xB1 0x05]
                                   :sub [:password :result]}}
               :normal-connection {:req {:cmd [0xc0 0x08]
                                         :sub [:password :timestamp]}
                                   :rsp {:cmd [0xD0 0x05]
                                         :sub [:password :result]}}
               :serial-number {:req {:cmd [0xC6 0x04]
                                     :sub [:password]}
                               :rsp {:cmd [0xD6 0x0C]
                                     :sub [:password :serial]}}
               :init-data-sync {:req {:cmd [0xc1 0x04]
                                      :sub [:password ]}
                                :rsp {:cmd [0xd1 0x07]
                                      :sub [:password :result :count]}}
               :reset {:req {:cmd [0xC4 0x04]
                             :sub [:password]}
                       :rsp {:cmd [0xD4 0x05]
                             :sub [:password :result]}}
               :read-data {:req {:cmd [0xC3 0x06]
                                 :sub [:password :index]}
                           :rsp {:cmd [0xD2 0x0C]
                                 :sub [:password :index :timestamp :activity]}}
               :testmode {:req {:cmd [0xE0 0x05]
                                 :sub [:password :testmode-onoff]}
                           :rsp {:cmd [0xF0 0x05]
                                 :sub [:password :result]}
                           :rsp2 {:cmd [0xF3 0x07]
                                  :sub [:password :activity :count]}}
               :raw-data-mode {:req {:cmd [0xE1 0x05]
                                     :sub [:password :rawmode-onoff]}
                               :rsp {:cmd [0xF0 0x05]
                                     :sub [:password :result]}
                               :rsp2 {:cmd [0xF2 0x10]
                                      :sub [:password :x :y :z]}}})


(defn ->buffer [data]
  (.from js/Buffer (js/Uint8Array. data)))

(defn ->byte-array [data]
  (.from js/Uint8Array data))

(defn timestamp []
  (->> (/ (.now js/Date) 1000)
       (iterate #(bit-shift-right % 8))
       (take 4)
       (map #(bit-and 0xff %))
       reverse))
  

(defn val->byte-array [v sz]
  (->> v
       (iterate #(bit-shift-right % 8))
       (take sz)
       (map #(bit-and 0xff %))
       reverse))

(defn ts-locale-fmt [v]
  ;; (let [date (js/Date. (* v 1000))]
    ;; {:year (.getFullYear date)
    ;;  :month (->> (.getMonth date)
    ;;              (get ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]))
    ;;  :day (.getDate date)
    ;;  :hour (.getHours date)
    ;;  :min (.getMinutes date)
  ;;  :sec (.getSeconds date)}))
  ;; (prn "timestamp " v)
  (.toLocaleString (js/Date. (* 1000 v))))

(defn ->float [val]
  ;; (prn "float " val)
  (-> (.from js/Buffer (js/Uint8Array. val))
      (.readFloatBE 0)
      (.toFixed 2)))

;;big endian order
(defn byte-array->val [data k]
  ;; (prn "byte-array->val ")
  (let [val (if (= :raw (get-in keyword-sz [k :type]))
              data
              (loop [v data
                     cnt (count data)
                     acc 0] 
                (if (zero? cnt)
                  acc
                  (recur (rest v) (dec cnt) (bit-or acc (bit-shift-left (first v) (* 8 (- cnt 1))))))))]
    (condp = k
      :timestamp (ts-locale-fmt val)
      :x (->float val)
      :y (->float val)
      :z (->float val)
      val)))


(defn req-keyword->val [req k]
  (condp = k
    :timestamp (timestamp)
    :password [0x30 0x30 0x30 0x30]
    :index (let [idx (get-in req [:info k])]
             (val->byte-array idx (get-in keyword-sz [k :size])))
    :testmode-onoff (let [onoff (get-in req [:info k])]
                      (val->byte-array onoff (get-in keyword-sz [k :size])))
    :rawmode-onoff (let [onoff (get-in req [:info k])]
                      (val->byte-array onoff (get-in keyword-sz [k :size])))
    
    [0x00]
    ))


;;arity ex {:cmd init :info {:index 0}}
(defn req [req]
  (let [{:keys [cmd sub]} (get-in protocol [(:cmd req) :req])
        converting (fn [src]
                     (reduce (fn [acc k]
                               (if (keyword? k)
                                 (apply conj acc {k (req-keyword->val req k)})
                                 (conj acc k))) [] src))
        v (apply conj cmd (flatten (map second (converting sub))))]

    (prn "req: " (:cmd req) (f/->hex cmd) (converting sub))
    ;; (prn "dec: " v)
    
    (-> (apply conj cmd (flatten (map second (converting sub)))) ;;(apply conj cmd (flatten (converting sub)))
        ->buffer)))



;;FIXIT ;;password should be raw. not convert.
(defn extract [data k]
  ;; (prn "extract")
  ;; (prn k data)
  (let [[cur remain] (split-at (get-in keyword-sz [k :size]) data)
        val (byte-array->val cur k)]
    ;; (prn "key: " k " val: " val)
    [val remain]))

;;decoding sub protocol
(defn resp-decoding [data protocol]
  (loop [d data
         p protocol
         acc {}]
    (let [[matched remain] (extract d (first p))]
      (if (empty? remain)
        (assoc acc (first p) matched)
        (recur remain (rest p) (assoc acc (first p) matched))))))

(defn rsp [resp]
  ;; (prn "received " (->byte-array resp))
  (let [[cmd sub] (->> resp ->byte-array (split-at 2))
        protocol (first (filter #(= cmd (:cmd %)) (->> (vals protocol)
                                                       (map #(-> % :rsp)))))]
    ;; (prn "cmd" cmd sub)
    ;; (prn "sub " sub)
    ;; (prn "pro" protocol)
    ;; (prn (:sub protocol))
    (prn "resp: " (f/->hex cmd) (f/->hex sub))
    (let [r (-> (resp-decoding sub (:sub protocol))
                (assoc :cmd cmd))]
      (prn "resp decoded" r)
      r)))
  
(defn rsp-testmode [resp]
  (prn "rsp testmode " (->> resp ->byte-array (split-at 2)))
  (let [[cmd sub] (->> resp ->byte-array (split-at 2))
        r (-> (resp-decoding sub (get-in protocol [:testmode :rsp2 :sub]))
              (assoc :cmd cmd))]
    (prn "test mode decoing " (f/->hex cmd))
    r))

(defn rsp-rawmode [resp]
  ;; (prn "rsp testmode " (->> resp ->byte-array (split-at 2)))
  (let [[cmd sub] (->> resp ->byte-array (split-at 2))
        r (-> (resp-decoding sub (get-in protocol [:raw-data-mode :rsp2 :sub]))
              (assoc :cmd cmd))]
    ;; (prn "test mode decoing " (f/->hex cmd))
    r))

  
(def e [209 7 48 48 48 48 1 0 14])


