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
     (if-let [href (.-href el)]
       (if-let [target (.-target el)]
          [href target]
          (when-let [parent (.-parentNode el)]
             (recur parent)))
       (when-let [parent (.-parentNode el)]
             (recur parent)))) (.-target e)))

(defn- locate-route [routes needle]
  (some
   (fn [route]
     (when (secretary/route-matches route needle)
       route))
   routes))

(defn- prevent-reload-on-known-path
  "Create a click handler that blocks page reloads for known routes in
  Secretary."
  [history routes]
  (events/listen
   js/document
   "click"
   (fn [e]
     (let [[href target] (find-a-attrs e)
           parsed-uri (.parse Uri href)
           path (.getPath parsed-uri)
           domain (.getDomain parsed-uri)
           title (.-title (.-target e))]
       (when (and (or (= domain "")                    ; the domain is empty
                      (= domain (.. js/window -location -hostname))) ; or is the same of the current domain
                  (or (empty? target)                  ; the target of the a tag is empty
                      (= target "_self"))              ; or is _self
                  (locate-route routes path))          ; path is handled by secretary
         (. history (setToken path title))
         (.preventDefault e))))))

; this is needed as of this
; https://code.google.com/p/closure-library/source/detail?spec=svn88dc096badf091f380b4c2b4a6514184511de657&r=88dc096badf091f380b4c2b4a6514184511de657
; setToken doen't replace the query string, it only attach it at the end
; solution here: https://github.com/Sparrho/supper/blob/master/src-cljs/supper/history.cljs
(defn- build-transformer
  "Custom transformer is needed to replace query parameters, rather
  than adding to them.
  See: https://gist.github.com/pleasetrythisathome/d1d9b1d74705b6771c20"
  []
  (let [transformer (goog.history.Html5History.TokenTransformer.)]
    (set! (.. transformer -retrieveToken)
          (fn [path-prefix location]
            (str (.-pathname location) (.-search location))))
    (set! (.. transformer -createUrl)
          (fn [token path-prefix location]
            (str path-prefix token)))
    transformer))

(defn configure-navigation!
  "Create and configure HTML5 history navigation."
  [secretary-dispatcher routes]
  (let [history (goog.history.Html5History. js/window (build-transformer))]
    (.setUseFragment history false)
    (.setPathPrefix history "")
    (.setEnabled history true)
    (dispatch-on-navigate history secretary-dispatcher)
    (prevent-reload-on-known-path history routes)))