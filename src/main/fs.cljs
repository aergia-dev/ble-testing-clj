(ns main.fs
  (:require ["fs" :as fs]
            ["util" :refer [promisify]]
            [cljs.core.async :refer [chan]])
  (:require-macros [cljs.core.async.macros :as m :refer [go go-loop]]))


(def log (chan))

(defn map->csv [m]
  (let [k (keys m)
        v (vals m)
        converted (->> (interleave (map name k) v)
                       (clojure.string/join ","))]
    (str converted ",\r\n")))


(go-loop []
  (when-let [log (<! log)]
    (let [{:keys [filename contents]} log]
      ;; (prn "#### " log)
      ;; (prn "contents " contents)
      (.appendFile fs filename contents (fn [err]
                                          ))
      (recur))))

   
(defn ->log [{:keys [type name mac contents]}]
  (go
    (let [l (condp = type
              :normal-data {:filename (str name "-" mac ".csv") 
                            :contents (map->csv (dissoc contents :password :cmd))}
              :rawmode-data {:filename (str "raw-" name "-" mac ".csv")
                             :contents (map->csv contents)}
              v)]
    (>! log l))))


    
