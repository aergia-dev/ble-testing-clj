(ns renderer.funcs
  (:require [re-frame.core :refer [dispatch]]))


(defn add-ipc-event []
  (.receive (.-api window) "from-main" (fn [data]
                                         (dispatch [:from-main (js->clj data)]))))
