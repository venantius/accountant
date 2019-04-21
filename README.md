# Accountant

Accountant is a ClojureScript library to make navigation in single-page
applications simple.

By default, clicking a link in a ClojureScript application that isn't a simple
URL fragment will trigger a full page reload. This defeats the purpose of using
such elegant frameworks as Om, Reagent, et al.

With Accountant, links that correspond to defined routes will trigger
dispatches to those routes and update the browser's path, but won't reload the
page.

Accountant also lets you navigate the app to a new URL directly, rather than through
`<a>` tags.

Be aware that Accountant relies on the browser's HTML5 history, so older
browsers will be left behind.

## Installation

Just add the following to your `project.clj`:

```clojure
:dependencies [venantius/accountant "0.2.4"]
```

## Usage

All you have to do to get Accountant working is the following:

```clojure
(ns your-app-ns
  (:require [accountant.core :as accountant]))

(accountant/configure-navigation! 
  {:nav-handler   (fn [path] ...) 
   :path-exists?  (fn [path] ...)})
```

The `:nav-handler` value is a fn of one argument, the path we're about to navigate to. You'll want to make whatever side-effect you need to render the page here. If you're using secretary, it'd look something like:

```clojure
(fn [path]
  (secretary/dispatch! path))
```

If you're using bidi + just rendering via react, that might look like:

```clojure
(fn [path]
  (om/update! app [:path] path))
```

The `:path-exists?` value is a fn of one argument, the path that we're about to navigate to.
The fn should return truthy if the path is
handled by your SPA, because accountant will call `event.preventDefault()` to
prevent the browser from doing a full page request.

Using secretary, `:path-exists?` should have a value like:

```clojure
(fn [path]
  (secretary/locate-route path))
```

Using bidi, it would look like:

```clojure
(fn [path]
  (boolean (bidi/match-route app-routes path)))
```

By default, clicking a link to the currently active route will not trigger the
navigation handler. You can disable this behavior and always trigger the
navigation handler by setting `reload-same-path?` to true during configuration.

```clojure
(accountant/configure-navigation! {:nav-handler (fn [path] ...)
                                   :path-exists? (fn [path] ...)
                                   :reload-same-path? true})
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

To cleanup the resources allocated by `configure-navigation!`, use `unconfigure-navigation!`. This is useful
in cases where you create a component that manages configuring navigation, and would like to be able to easily
start/stop it.

```clojure
(accountant/unconfigure-navigation!)
```

### Caveat: UI Frameworks
Sometimes links may be used nested within UI components, especially when using third-party wrappers, like [react-bootstrap](https://react-bootstrap.github.io/) etc. These links may have an empty `href` attribute or a value like `#`. Two things might happen: Either, if a route is defined for the root path (i.e. '/' or '/#'), accountant will suppress the browser navigation and dispatch via secretary or the browser will reload the page.

To prevent this accountant looks for an attribute `data-trigger` on every link. The presence of this attribute signals that this link is a means to trigger a callback, not a navigation. If `data-trigger` is defined on a link it gets completely ignored, just like a button.

Example: When using a [DropdownButton](https://react-bootstrap.github.io/components.html#btn-dropdowns) with [MenuItems](https://react-bootstrap.github.io/components.html#menu-items) each item will contain an `<a href="#"...>` element. Since this element can't be replaced, we can at least add arbitrary attributes to it:

```clojure
(let [dropdown-button (reagent/adapt-react-class js/ReactBootstrap.DropdownButton)
      menuitem (reagent/adapt-react-class js/ReactBootstrap.MenuItem)]
  [dropdown-button {:id "foo" :title "..." :onSelect (fn [idx]...)}
    [menuitem {:id "1"
               :data-trigger true
               :eventKey "1"}]
```

## License

Copyright © 2017 W. David Jarvis

Distributed under the Eclipse Public License, the same as Clojure.
