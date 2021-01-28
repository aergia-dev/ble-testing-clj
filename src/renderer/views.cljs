(ns renderer.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [clojure.string :as str]
            [renderer.ipc :refer [send-ipc]]))

(defn btn-device-list []
  [:button {:on-click #(send-ipc {:cmd :bt
                                  :msg :scan})}
   "scan"])


(defn ui
  []
  [:div
   [:h1 "clj electron template"]
   [btn-device-list]])

