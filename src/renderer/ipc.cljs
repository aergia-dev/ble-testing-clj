(ns renderer.ipc
  (:require [re-frame.core :refer [dispatch]]))


(defn add-ipc-event []
  (.receive (.-api js/window) "fromMain" (fn [data]
                                            (prn "from main" data)
                                            (dispatch [:from-main (js->clj data)]))))

(defn send-ipc [data]
  (prn "send ipc" data)
  (.send (.-api js/window) "toMain" (clj->js data)))
