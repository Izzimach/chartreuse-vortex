# chartreuse-vortex
Use immutable data structures in [ClojureScript](https://github.com/clojure/clojurescript) to drive many dynamic sprites.

How many sprites can we drive using ClojureScript? Just building a vector of sprite data and shoving it at Om/React can get a few dozen at 60fps, but there is tons of overhead in conversion and building of spurious objects. Surely we can get a few more sprites in the screen.

Built using [om-react-pixi](https://github.com/Izzimach/om-react-pixi).

![sprites sprites sprites](docs/screenshot.png)
