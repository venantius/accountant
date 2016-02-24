# Accountant

Accountant is a ClojureScript library to make navigation in single-page
applications simple.

By default, clicking a link in a ClojureScript application that isn't a simple
URL fragment will trigger a full page reload. This defeats the purpose of using
such elegant frameworks as Om, Reagent, et al.

With Accountant, links that correspond to defined Secretary routes will trigger
dispatches to those routes and update the browser's path, but won't reload the
page.

Accountant also lets you navigate the app to a new URL directly, rather than through
`<a>` tags.

Be aware that Accountant relies on the browser's HTML5 history, so older
browsers will be left behind.

## Installation

Just add the following to your `project.clj`:

```clojure
:dependencies [venantius/accountant "0.1.7"]
```

## Usage

All you have to do to get Accountant working is the following:

```clojure
(ns your-app-ns
  (:require [accountant.core :as accountant]))

(accountant/configure-navigation! {:nav-handler (fn [path] ...) :path-exists? (fn [path] ...)})
```

nav-handler is a fn of one argument, the path we're about to navigate to. You'll want to make whatever side-effect you need to render the page here. If you're using secretary, it'd look something like:

```clojure
(fn [path]
  (secretary/dispatch! path))
```

If you're using bidi + just rendering via react, that might look like:

```clojure
(fn [path]
  (om/update! app [:path] path))
```

`path-exists?` is a fn of one argument, a path, that takes the path
we're about to navigate to. This should return truthy if the path is
handled by your SPA, because accountant will preventDefault the event, to
prevent the browser from doing a full page request.

Using secretary, path-exists? would look like:

```clojure
(fn [path]
  (secretary/locate-route path))
```

Using bidi, path-exists? would look like:

```clojure
(fn [path]
  (boolean (bidi/match-route app-routes path)))
```

You can also use Accountant to set the current path in the browser, e.g.

```clojure
(accountant/navigate! "/foo/bar/baz")
```

If you want to dispatch the current path, just add the following:

```clojure
(dispatch-current!)
```

Note that both `navigate!` and `dispatch-current!` can only be used after calling `configure-navigation!`

## License

Copyright © 2015 W. David Jarvis

Distributed under the Eclipse Public License, the same as Clojure.
