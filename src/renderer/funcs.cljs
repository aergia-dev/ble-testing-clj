(ns renderer.funcs
  (:require [re-frame.core :refer [dispatch]]))


;; (defn add-ipc-event []
  ;; (.receive (.-api window) "from-main" (fn [data]
                                         ;; (dispatch [:from-main (js->clj data)]))))

(defn obj->clj
  [obj]
  (-> (fn [acc key]
        (let [v (goog.object/get obj key)]
          (if (= "function" (goog/typeOf v))
            acc
            (assoc acc (keyword key) (obj->clj v)))))
      (reduce {} (.getKeys goog.object obj))))
