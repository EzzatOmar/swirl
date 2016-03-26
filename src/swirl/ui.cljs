(ns swirl.ui
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent]))

(defn trigger-eval
  [text-ch text]
  (a/put! text-ch text))

(defn stop-watch
  [{:keys [text* autobuild]}]
  (remove-watch text* :put-text)
  (reset! autobuild false))

(defn start-watch
  [{:keys [text* text-ch autobuild]}]
  (add-watch 
   text* :put-text
   (fn put-text [_ _ _ text]
     (trigger-eval text-ch text)))
  (reset! autobuild true))

(defn toggle-autobuild
  [{:keys [autobuild] :as context}]
  (if @autobuild
    (stop-watch context)
    (start-watch context)))

(defn autobuild-toggle
  [{:keys [autobuild] :as context}]
  [:label
    [:input
     {:type "checkbox"
      :checked @autobuild
      :on-change #(toggle-autobuild context)}]
    "autobuild"])

(defn build-button
  [{:keys [text-ch text*]}]
  [:button
   {:on-click #(trigger-eval text-ch @text*)}
   "build"])

(defn textarea
  [{:keys [text*]}]
  [:textarea
   {:value @text*
    :on-change 
    (fn [e]
      (reset! text* (.. e -target -value)))}])

(defn log
  [{:keys [history*]}]
  [:div @history*])

(defn controls
  [context]
  [:div
   [autobuild-toggle context]
   [build-button context]])

(defn app
  [context]
  [:div#app
   [textarea context]
   [controls context]
   [log context]])

(defn start-render
  [{:keys [mount-pt] :as context}]
  (reagent/render-component
   [app context]
   mount-pt)
  (fn stop-render
    []
    (reagent/render-component
     [:div "stopped"]
     mount-pt)))

(defn start
  [context]
  (let [stop-render (start-render context)]
    (fn stop
      []
      (stop-render)
      (stop-watch context))))

(defn component
  [text* history* mount-pt]
  (let [text-ch (a/chan)
        context {:text* text*
                 :history* history*
                 :text-ch text-ch
                 :autobuild (reagent/atom false)
                 :mount-pt mount-pt}
        stop-fn* (atom (constantly nil))
        stop! (fn [] (@stop-fn*))
        start! (fn []
                 (stop!)
                 (reset! stop-fn* (start context)))]
    {:text-ch text-ch
     :start-ui! start!
     :stop-ui! stop!}))
