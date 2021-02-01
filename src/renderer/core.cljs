(ns renderer.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [re-frame.core :as rf :refer [dispatch]]
            [clojure.string :as str]  
            [devtools.core :as devtools] 
            [renderer.views :refer [ui]]
            [renderer.subs]
            [renderer.events]
            [renderer.db]
            [renderer.ipc :refer [add-ipc-event]]))

(devtools/install!)       ;; we love https://github.com/binaryage/cljs-devtools
(enable-console-print!)

;; -- Entry Point -------------------------------------------------------------*

(defn ^:export init
  []
  (add-ipc-event)
  (rf/dispatch-sync [:initialize])  
  (rdom/render [renderer.views/ui]           
                  (js/document.getElementById "app-container")))

(defn stop []
  )

(defn start []
  (prn "re-load")
  (rf/dispatch-sync [:initialize]))

