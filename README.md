# async-test

A series of ClojureScript experiments with core.async.

## Usage

Clone the [core.async](http://github.com/clojure/core.async) to a
convenient location. `cd` into the repo and run `lein install`.

Then clone this repo into a convenient location and `cd` into it.

The examples need ClojureScript from master as it contains a bugfix.
Clone [Clojurescript](http://github.com/clojure/clojurescript) into
the directory.

You can now build all of the examples from the project directory
with:

```
lein cljsbuild once NAME
```

You can see `project.clj` for the names of the different examples. All
of the examples can be run after building by opening
`NAME.html`.

## Contributions

I'm not taking pull requests for this project. It's a personal
core.async playground. While I hope people find some of these examples
informative that is not the primary goal.

## License

Copyright © 2013 David Nolen

Distributed under the Eclipse Public License, the same as Clojure.
