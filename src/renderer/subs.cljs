(ns renderer.subs
  (:require [re-frame.core :as rf :refer [reg-sub subscribe]]))


(reg-sub
 :ex
 (fn [db _]
   (:nil db)))

(reg-sub
 :devices
 (fn [db _]
   (:devices db)))
