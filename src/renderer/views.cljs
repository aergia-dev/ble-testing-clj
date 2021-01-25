(ns renderer.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [clojure.string :as str]
            [renderer.ipc :refer [send-ipc]]))

(defn test-btn []
  [:button {:on-click #(send-ipc {:cmd :test
                                  :msg "test msg"})}
   "test btn"])

(defn ui
  []
  [:div
   [:h1 "clj electron template"]
   [test-btn]])

