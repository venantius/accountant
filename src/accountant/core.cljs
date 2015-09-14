(ns accountant.core
  "The only namespace in this library."
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! <! chan]]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.history.Html5History
           goog.Uri))

(defn- listen [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn- dispatch-on-navigate
  [history]
  (let [navigation (listen history EventType/NAVIGATE)]
    (go
      (while true
        (let [token (.-token (<! navigation))]
          (secretary/dispatch! token))))))

(defn- find-href
  "Given a DOM element that may or may not be a link, traverse up the DOM tree
  to see if any of its parents are links. If so, return the href content."
  [e]
  ((fn [e]
     (if-let [href (.-href e)]
        href
        (when-let [parent (.-parentNode e)]
           (recur parent)))) (.-target e)))

(defn- prevent-reload-on-known-path
  "Create a click handler that blocks page reloads for known routes in
  Secretary."
  [history]
  (events/listen
   js/document
   "click"
   (fn [e]
     (let [href (find-href e)
           path (.getPath (.parse Uri href))
           title (.-title (.-target e))]
       (when (secretary/locate-route path)
         (. history (setToken path title))
         (.preventDefault e))))))

(defn configure-navigation!
  "Create and configure HTML5 history navigation."
  []
  (let [history (Html5History.)]
    (.setUseFragment history false)
    (.setPathPrefix history "")
    (.setEnabled history true)
    (dispatch-on-navigate history)
    (prevent-reload-on-known-path history)))
