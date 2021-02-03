(ns renderer.events
  (:require [re-frame.core  :as rf :refer [reg-event-db reg-event-fx inject-cofx path after]]
            [cljs.spec.alpha :as s]
            [renderer.ipc :refer [send-ipc]]
            [renderer.funcs :as f]))



;; -- Domino 2 - Event Handlers -----------------------------------------------

(reg-event-db              ;; sets up initial application state
 :initialize                 ;; usage:  (dispatch [:initialize])
 (fn [_ _]                   ;; the two parameters are not important here, so use _
   {:devices {}}))

(reg-event-db
 :to-main
 (fn [db [_ cmd dev-name]]
   (prn cmd)
   (condp = cmd
     :scan (do
             (send-ipc {:type :bt
                        :cmd :scan
                        :info nil})
             (assoc db :devices {}))
     :data-sync (do
                  (send-ipc {:type :bt
                             :cmd :data-sync
                             :info {:mac (get-in db [:devices dev-name :mac])}}))
     :reset (do
              (send-ipc {:type :bt
                         :cmd :reset
                         :info {:mac (get-in db [:devices dev-name :mac])}}))
     :testmode-on (do
                    (send-ipc {:type :bt
                               :cmd :testmode
                               :info {:mac (get-in db [:devices dev-name :mac])
                                      :testmode-onoff 1}}))
     :testmode-off (do
                    (send-ipc {:type :bt
                               :cmd :testmode
                               :info {:mac (get-in db [:devices dev-name :mac])
                                      :testmode-onoff 0}}))

     :raw-on (do
                    (send-ipc {:type :bt
                               :cmd :rawmode
                               :info {:mac (get-in db [:devices dev-name :mac])
                                      :raw-onoff 1}}))
     :raw-off (do
                    (send-ipc {:type :bt
                               :cmd :rawmode
                               :info {:mac (get-in db [:devices dev-name :mac])
                                      :raw-onoff 0}}))

     db)))
             

(reg-event-db
 :from-main
 (fn [db [_ data]]
   (let [{:keys [cmd contents]} data]
     (condp = cmd
       "device" (let [{:keys [name mac]} contents]
                  (prn name)
                  (assoc-in db [:devices name] {:latest-sync "not once"
                                                :active false
                                                :mode "normal"
                                                :testmode false
                                                :mac mac}))
       "data-sync" (do
                     (prn cmd)
                     (prn contents))
       "rawmode-off" (do
                       (prn "raw mode off"))
       db))))


(reg-event-db
 :clear-device-list
 (fn [db _]
   (assoc db :devices [])))

(reg-event-db
 :update-active
 (fn [db [_ dev-name val]]
   (assoc-in db [:devices dev-name :active] val)))

(reg-event-db
 :update-mode
 (fn [db [_ dev-name val]]
   (assoc-in db [:devices dev-name :mode] val)))

