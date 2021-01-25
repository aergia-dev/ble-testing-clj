(ns main.core
  (:require ["electron" :refer [app BrowserWindow ipcMain crashReporter dialog]]
            ["fs" :as fs]
            [main.ipc :refer [receive-ipc]]))

(def main-window (atom nil))

(defn init-browser []
  (reset! main-window (BrowserWindow.
                        (clj->js {:width 700
                                  :height 600
                                  :webPreferences {:nodeIntegration false
                                                   :enableRemoteModule false
                                                   :contextIsolation true
                                                   :preload (str js/__dirname "/preload.js")
                                                   :devTools true
                                                   :sandbox true}})))
  ; Path is relative to the compiled js file (main.js in our case)
  (.loadURL @main-window (str "file://" js/__dirname "/public/index.html"))
  (.. @main-window -webContents (openDevTools))
  (.on @main-window "closed" #(reset! main-window nil)))

(defn main []
  (.start crashReporter
          (clj->js
            {:companyName "nil"
             :productName "nil"
             :submitURL "nil"
             :compress true
             :autoSubmit false}))

  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-browser)
  (.on ipcMain "toMain" (fn [event args]
                          (receive-ipc event args))))


