# Accountant

Accountant is a ClojureScript library to make navigation in single-page
applications easy.

By default, clicking a link in a ClojureScript application that isn't a simple
URL fragment will trigger a full page reload. This defeats the purpose of using
such elegant frameworks as Om, Reagent, et al.

With Accountant, links that correspond to defined Secretary routes trigger
dispatches to those routes and update the browser's path, but don't reload the
page.

Be aware this relies on the browser's HTML5 history, so older browsers will be
left behind.

## Installation

Just add the following to your `project.clj`:

```clojure
:dependencies [venantius/accountant "0.1.0"]
```

## Usage

All you have to do to get Accountant working is the following:

```clojure
(ns your-app-ns
  (:require [accountant.core :as accountant]))

(accountant/configure)
```

...and you're good to go!

## License

Copyright © 2015 W. David Jarvis

Distributed under the Eclipse Public License, the same as Clojure.
