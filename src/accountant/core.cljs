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
  [history secretary-dispatcher]
  (let [navigation (listen history EventType/NAVIGATE)]
    (go
      (while true
        (let [token (.-token (<! navigation))]
          (secretary-dispatcher token))))))

(defn- find-a-attrs
  "Given a DOM element that may or may not be a link, traverse up the DOM tree
  to see if any of its parents are links. If so, return the href and the target content."
  [e]
  ((fn [el]
     (if-let [href (.-href el)
              target (.-target el)]
        [href target]
        (when-let [parent (.-parentNode el)]
           (recur parent)))) (.-target e)))

(defn- prevent-reload-on-known-path
  "Create a click handler that blocks page reloads for known routes in
  Secretary."
  [history route-locator]
  (events/listen
   js/document
   "click"
   (fn [e]
     (let [[href target] (find-a-attrs)
           parsed-uri (.parse Uri href)
           path (.getPath parsed-uri)
           domain (.getDomain parsed-uri)
           title (.-title (.-target e))]
       (when (and (or (= domain "")                    ; the domain is empty
                      (= domain (.. js/window -location -hostname))) ; or is the same of the current domain
                  (or (nil? target)                    ; the target of the a tag is empty
                      (= target "")                    ;   "     "     "     "
                      (= target "_self"))              ; or is _self
                  (route-locator path))                ; path is handled by secretary
         (. history (setToken path title))
         (.preventDefault e))))))

(defn configure-navigation!
  "Create and configure HTML5 history navigation."
  [secretary-dispatcher route-locator]
  (let [history (Html5History.)]
    (.setUseFragment history false)
    (.setPathPrefix history "")
    (.setEnabled history true)
    (dispatch-on-navigate history secretary-dispatcher)
    (prevent-reload-on-known-path history route-locator)))
