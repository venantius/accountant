# Accountant

Accountant is a ClojureScript library to make navigation in single-page
applications simple. It expects you to use [Secretary](https://github.com/gf3/secretary) to define your routes.

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
:dependencies [venantius/accountant "0.1.6"]
```

## Usage

All you have to do to get Accountant working is the following:

```clojure
(ns your-app-ns
  (:require [accountant.core :as accountant]))

(accountant/configure-navigation!)
```

...and you're good to go!

You can also use Accountant to set the current path in the browser, e.g.

```clojure
(accountant/navigate! "/foo/bar/baz")
```

If you want to dispatch the current path, just add the following:

```clojure
(dispatch-current!)
```

## License

Copyright © 2015 W. David Jarvis

Distributed under the Eclipse Public License, the same as Clojure.
