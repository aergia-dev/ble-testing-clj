(ns renderer.views
  (:require [reagent.core  :as reagent]
            [re-frame.core :as rf :refer [subscribe dispatch]]
            [clojure.string :as str]))

(defn interval-sync []
  )

(defn btn-device-list []
  [:div {:class "field is-grouped"}
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :scan])}
     "scan"]]
   [:p {:class "control"}
    [:input {:class "input is-info is-small"
             :type "text"
             :placeholder "interval time (minute)"}]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click interval-sync}
     "interval sync"]]])

(defn btn-single-device-cmd []
  [:div {:class "field is-grouped"}
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :register])}
     "register"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :de-register])}
     "de-register"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :normal-connection])}
     "normal connection"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :normal-reset])}
     "normal reset"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :serial])}
     "serial"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :data-sync-init])}
     "data sync init"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :sync])}
     "sync"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :test-mode])}
     "test mode"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :raw-data-mode])}
     "raw data mode"]]
   [:p {:class "control"}
    [:button {:class "button is-primary is-small"
              :on-click #(dispatch [:to-main :raw-data-mode])}
     "raw data mode"]]])
   


(defn checkbox-onchange [e nm]
  (let [active (-> e (.-target) (.-checked))]
    (dispatch [:update-active nm active])))

(defn select-onchange [e nm]
  (let [mode (-> e (.-target) (.-value))]
    (dispatch [:update-mode nm mode])))

(defn device-list []
  (let [dev-lst @(subscribe [:devices])]
    [:div
     (when (not (empty? dev-lst))
       (for [dev dev-lst]
         (let [[name {:keys [latest-sync active mode mac]}] dev]
             [:div {:class "container"
                    :key (str "div-k-" name)}
              [:label {:class "checkbox"}
               [:input {:type "checkbox"
                        :on-change #(checkbox-onchange % name)
                        :key (str "key-" name)
                        :name (str  "-checkbox")}]
               (str name "  " mac "  " latest-sync)]
              
              [:div {:class "field is-grouped"}
                [:div {:class "select is-small"}
                 [:select {:name "mode"
                           :id "id-mode"
                           :defaultValue mode
                           :on-change #(select-onchange % name)}
                  [:option "normal"]
                  [:option "test"]]]
               (condp = mode
                 "normal" [:p {:class "control"}
                           [:button {:class "button is-small"
                                     :on-click #(dispatch [:to-main :data-sync name])} "data sync"]]
                 "test" [:p {:class "control"}
                         [:button {:class "button is-small"
                                   :on-click nil} "test start"]])
               [:p {:class "control"}
                [:button {:class "button is-primary is-small"
                          :on-click #(dispatch [:to-main :reset name])}
                 "reset"]]]])
           ))]))

     
(defn ui
  []
  [:div
   [btn-device-list]
   [device-list]
   [btn-single-device-cmd]])

