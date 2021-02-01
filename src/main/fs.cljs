(ns main.fs
  (:require ["fs" :as fs]
            ["util" :refer [promisify]]
            [cljs.core.async :refer [chan]])
  (:require-macros [cljs.core.async.macros :as m :refer [go go-loop]]))


(def log (chan))

(go-loop []
  (when-let [contents (<! log)]
    (let [c (get contents :contents)
          filename (str (:name c) "-" (:mac c) ".txt")]
      (prn "in log " contents)
      (prn c)
      (prn "filename" filename)
      (.appendFile fs filename (:data c) (fn [err]
                                      (do
                                        (prn "error in saving a log")
                                        (prn err)
                                        ;; (append f err "log-err.txt" (fn [err]
                                                                       ))))
    (recur)))
  

(defn ->log [l]
  (go
    (>! log l)))


    
