(ns renderer.subs
  (:require [re-frame.core :as rf :refer [reg-sub subscribe]]))


(rf/reg-sub
 :ex
 (fn [db _]
   (:nil db)))
