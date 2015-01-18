(ns hobbes.core
  "Hobbes main namespace.
  Manages the whole program by stitching other namespaces together."
  (:require [hobbes.utils :refer :all]
            [hobbes.preprocessor :as pre]
            [hobbes.engine :as engine])
  (:gen-class))

(defn -main
  "Main entry point to Hobbes"
  [& args]
  ())
