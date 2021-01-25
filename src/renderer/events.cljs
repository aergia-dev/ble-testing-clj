(ns renderer.events
  (:require
   [re-frame.core  :as rf :refer [reg-event-db reg-event-fx inject-cofx path after]]
   [cljs.spec.alpha :as s]))



;; -- Domino 2 - Event Handlers -----------------------------------------------

(reg-event-db              ;; sets up initial application state
 :initialize                 ;; usage:  (dispatch [:initialize])
 (fn [_ _]                   ;; the two parameters are not important here, so use _
   {:nil []}))

(reg-event-db
 :from-main
 (fn [db {:keys [k v] :as data}]
   (prn "data: " data)
   (assoc db k v)))
