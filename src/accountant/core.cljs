(ns accountant.core
  "The only namespace in this library."
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! <! chan]]
            [clojure.string :as str]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.history.Event
           goog.history.Html5History
           goog.Uri))

(defn- transformer-create-url
  "Replacement for googl.history.Html5History.TokenTransformer/createUrl that
  avoids preserving the query string, as Google's version incorrectly does. See
  https://groups.google.com/d/msg/closure-library-discuss/jY4yzKX5HYg/Ft1fPEO23r4J
  for reference."
  [token path-prefix _]
  (str path-prefix token))

(defn- transformer-retrieve-token
  "Replacement for goog.history.Html5History.TokenTransformer/retrieveToken that
  appends the hash fragment and query string (stripped by default) back to the
  path. See
  https://groups.google.com/d/msg/closure-library-discuss/jY4yzKX5HYg/Ft1fPEO23r4J
  for reference."
  [path-prefix location]
  (str (.-pathname location) (.-search location) (.-hash location)))

(defonce history
  (let [transformer goog.history.Html5History.TokenTransformer.]
    (set! (.. transformer -retrieveToken) transformer-retrieve-token)
    (set! (.. transformer -createUrl) transformer-create-url)
    (Html5History. js/window transformer)))

(defn- listen [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn- dispatch-on-navigate
  [history nav-handler]
  (let [navigation (listen history EventType/NAVIGATE)]
    (go
      (while true
        (let [token (.-token (<! navigation))]
          (nav-handler token))))))

(defn- find-href-node
  "Given a DOM element that may or may not be a link, traverse up the DOM tree
  to see if any of its parents are links. If so, return the node."
  [e]
  (if (.-href e)
    e
    (when-let [parent (.-parentNode e)]
      (recur parent))))

(defn- uri->query [uri]
  (let [query (.getQuery uri)]
    (when-not (empty? query)
      (str "?" query))))

(defn- uri->fragment [uri]
  (let [fragment (.getFragment uri)]
    (when-not (empty? fragment)
      (str "#" fragment))))

(defn- prevent-reload-on-known-path
  "Create a click handler that blocks page reloads for known routes"
  [history path-exists?]
  (events/listen
   js/document
   "click"
   (fn [e]
     (let [target (.-target e)
           button (.-button e)
           meta-key (.-metaKey e)
           alt-key (.-altKey e)
           ctrl-key (.-ctrlKey e)
           shift-key (.-shiftKey e)
           any-key (or meta-key alt-key ctrl-key shift-key)
           href-node (find-href-node target)
           href (when href-node (.-href href-node))
           link-target (when href-node (.-target href-node))
           uri (.parse Uri href)
           path (.getPath uri)
           query (uri->query uri)
           fragment (uri->fragment uri)
           relative-href (str path query fragment)
           title (.-title target)
           host (.getDomain uri)
           current-host js/window.location.hostname
           loc js/window.location
           current-relative-href (str (.-pathname loc) (.-query loc) (.-hash loc))]
       (when (and (not any-key)
                  (#{"" "_self"} link-target)
                  (= button 0)
                  (= host current-host)
                  (not= current-relative-href relative-href)
                  (path-exists? path))
         (.setToken history relative-href title)
         (.preventDefault e))))))

(defonce nav-handler nil)
(defonce path-exists? nil)

(defn configure-navigation!
  "Create and configure HTML5 history navigation.

  nav-handler: a fn of one argument, a path. Called when we've decided
  to navigate to another page. You'll want to make your app draw the
  new page here.

  path-exists?: a fn of one argument, a path. Return truthy if this path is handled by the SPA"
  [{:keys [nav-handler path-exists?]}]
  (.setUseFragment history false)
  (.setPathPrefix history "")
  (.setEnabled history true)
  (set! accountant.core/nav-handler nav-handler)
  (set! accountant.core/path-exists? path-exists?)
  (dispatch-on-navigate history nav-handler)
  (prevent-reload-on-known-path history path-exists?))

(defn map->params [query]
  (let [params (map #(name %) (keys query))
        values (vals query)
        pairs (partition 2 (interleave params values))]
    (str/join "&" (map #(str/join "=" %) pairs))))

(defn navigate!
  "add a browser history entry. updates window/location"
  ([route] (navigate! route {}))
  ([route query]
   (if nav-handler
     (let [token        (.getToken history)
           old-route    (first (str/split token "?"))
           query-string (map->params (reduce-kv (fn [valid k v]
                                                  (if v
                                                    (assoc valid k v)
                                                    valid)) {} query))
           with-params  (if (empty? query-string)
                          route
                          (str route "?" query-string))]
       (if (= old-route route)
         (.replaceToken history with-params)
         (.setToken history with-params)))
     (js/console.error "can't navigate! until configure-navigation! called"))))

(defn dispatch-current! []
  "Dispatch current URI path."
  (let [path (-> js/window .-location .-pathname)
        query (-> js/window .-location .-search)
        hash (-> js/window .-location .-hash)]
    (if nav-handler
      (nav-handler (str path query hash))
      (js/console.error "can't dispatch-current until configure-navigation! called"))))
