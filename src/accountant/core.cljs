(ns accountant.core
  "The only namespace in this library."
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! <! chan]]
            [clojure.string :as str]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.history.Event
           goog.history.Html5History
           goog.Uri))

(defonce history (Html5History.))

(defn- listen [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn- dispatch-on-navigate
  [secretary-dispatcher history]
  (let [navigation (listen history EventType/NAVIGATE)]
    (go
      (while true
        (let [token (.-token (<! navigation))]
          (secretary-dispatcher token))))))

(defn- find-a
  "Given a DOM element that may or may not be a link, traverse up the DOM tree
  to see if any of its parents are links. If so, return the href and the target content."
  [e]
  ((fn [el]
     (if-let [href (.-href el)]
       el
       (if-let [parent (.-parentNode el)]
         (recur parent)
         nil))) (.-target e)))

(defn- locate-route [routes needle]
  "check if route is handled by secretary routes stack"
  (some
    (fn [route]
      (when (secretary/route-matches route needle)
        route))
    routes))

(defn- get-url
  "Gets the URL for a history token, but without preserving the query string
  as Google's version incorrectly does. (See https://goo.gl/xwgUos)"
  [history token]
  (str (.-pathPrefix_ history) token))

(defn- set-token!
  "Sets a history token, but without preserving the query string as Google's
  version incorrectly does. (See https://goo.gl/xwgUos)"
  [history token title]
  (let [js-history (.. history -window_ -history)
        url (get-url history token)]
    (.pushState js-history nil (or title js/document.title "") url)
    (.dispatchEvent history (Event. token))))

(defn- uri->query [uri]
  (let [query (.getQuery uri)]
    (when-not (empty? query)
      (str "?" query))))

(defn- uri->fragment [uri]
  (let [fragment (.getFragment uri)]
    (when-not (empty? fragment)
      (str "#" fragment))))

(defn- prevent-reload-on-known-path
  "Create a click handler that blocks page reloads for known routes in
  Secretary."
  [routes-stack history]
  (events/listen
   js/document
   "click"
   (fn [e]
     (let [a-el (find-a e)
           href (.-href a-el)
           a-target (.-target a-el)]
       (if (empty? href)
         (.preventDefault e) ; prevent any action, probably handled by onClick
         (let [target (.-target e)
               button (.-button e)
               meta-key (.-metaKey e)
               alt-key (.-altKey e)
               ctrl-key (.-ctrlKey e)
               shift-key (.-shiftKey e)
               any-key (or meta-key alt-key ctrl-key shift-key)
               uri (.parse Uri href)
               path (.getPath uri)
               domain (.getDomain uri)
               query (uri->query uri)
               fragment (uri->fragment uri)
               relative-href (str path query fragment)
               title (.-title target)]
           (when (and (not any-key)          ; no special keys where pressed while clicking
                      (= button 0)           ; is left mouse button click
                      (or (empty? domain)    ; the domain is empty or is the same of the current domain
                          (= domain (.. js/window -location -hostname)))
                      (or (empty? a-target)                  ; the target of the a tag is empty
                          (= a-target "_self"))              ; or is _self
                      (locate-route routes-stack path))      ; the path is handled by secretary
             (set-token! history relative-href title)
             (.preventDefault e))))))))

(defn configure-navigation!
  "Create and configure HTML5 history navigation."
  [secretary-dispatcher routes-stack]
  (.setUseFragment history false)
  (.setPathPrefix history "")
  (.setEnabled history true)
  (dispatch-on-navigate secretary-dispatcher history)
  (prevent-reload-on-known-path routes-stack history))

(defn map->params [query]
  (let [params (map #(name %) (keys query))
        values (vals query)
        pairs (partition 2 (interleave params values))]
    (str/join "&" (map #(str/join "=" %) pairs))))

(defn navigate!
  "add a browser history entry. updates window/location"
  ([route] (navigate! route {}))
  ([route query]
     (let [token (.getToken history)
           old-route (first (str/split token "?"))
           query-string (map->params (reduce-kv (fn [valid k v]
                                                  (if v
                                                    (assoc valid k v)
                                                    valid)) {} query))
           with-params (if (empty? query-string)
                         route
                         (str route "?" query-string))]
       (if (= old-route route)
         (. history (replaceToken with-params))
         (. history (setToken with-params))))))

(defn dispatch-current! [secretary-dispatcher]
  "Dispatch current URI path."
  (let [path (-> js/window .-location .-pathname)
        query (-> js/window .-location .-search)
        hash (-> js/window .-location .-hash)]
    (secretary-dispatcher (str path query hash))))