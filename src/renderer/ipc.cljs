(ns renderer.ipc
  (:require [re-frame.core :refer [dispatch]]
            [renderer.funcs :refer [obj->clj]]))


(defn add-ipc-event []
  (.receive (.-api js/window) "fromMain" (fn [data]
                                           (prn "ipc received" (js->clj data :keywordize-keys true))
                                           (dispatch [:from-main (js->clj data :keywordize-keys true)]))))

(defn send-ipc [data]
  (prn "send ipc" data)
  (.send (.-api js/window) "toMain" (clj->js data)))
