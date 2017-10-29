(ns accountant.core
  "The only namespace in this library."
  (:require [clojure.string :as str]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.history.Event
           goog.history.Html5History
           goog.Uri))

(defn- transformer-create-url
  [token path-prefix location]
  (str path-prefix token))

(defn- transformer-retrieve-token
  [path-prefix location]
  (str (.-pathname location) (.-search location) (.-hash location)))

(defonce history (let [transformer (goog.history.Html5History.TokenTransformer.)]
                   (set! (.. transformer -retrieveToken) transformer-retrieve-token)
                   (set! (.. transformer -createUrl) transformer-create-url)
                   (Html5History. js/window transformer)))

(defn- dispatch-on-navigate
  [history nav-handler]
  (events/listen
    history
    EventType/NAVIGATE
    (fn [e]
      (let [token (.-token e)]
        (nav-handler token)))))

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
           port (.getPort uri)
           current-host js/window.location.hostname
           current-port js/window.location.port
           loc js/window.location
           current-relative-href (str (.-pathname loc) (.-query loc) (.-hash loc))]
       (when (and (not any-key)
                  (#{"" "_self"} link-target)
                  (= button 0)
                  (= host current-host)
                  (or (not port)
                      (= (str port) (str current-port)))
                  (path-exists? path))
         (when (not= current-relative-href relative-href) ;; do not add duplicate html5 history state
           (. history (setToken relative-href title)))
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
         (set-token! history with-params nil)))
     (js/console.error "can't navigate! until configure-navigation! called"))))

(defn dispatch-current! []
  "Dispatch current URI path."
  (let [path (-> js/window .-location .-pathname)
        query (-> js/window .-location .-search)
        hash (-> js/window .-location .-hash)]
    (if nav-handler
      (nav-handler (str path query hash))
      (js/console.error "can't dispatch-current until configure-navigation! called"))))
