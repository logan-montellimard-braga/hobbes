(ns hobbes.preprocessor
  "Engine used to preprocess input string for optimal parsing."
  (:require [clojure.string :as s]))

(defn- remove-comments
  "Remove comment lines (beginning with '!!') from input.
  Returns the treated input."
  [input]
  (->>
    input
    (s/split-lines)
    (remove #(re-matches #"^\s*!!.*" %))
    (interpose "\n")
    (apply str)))

(defn- trim-each-line
  "Remove trailing whitespaces and tabs from input, line by line.
  Returns the treated input."
  [input]
  (->>
    input
    (s/split-lines)
    (map s/trim)
    (interpose "\n")
    (apply str)))

(defn- add-padding-newlines
  "Add newlines to end of input if needed, to correctly treat last element,
  because special elements (ie: not paragraphs) are treated as paragraphs by
  the parser if there is no 2 trailing newlines after them.
  Returns the treated input."
  [input]
  (cond
    (.endsWith input "\n\n") input
    (.endsWith input "\n")   (str input "\n")
    :else                    (str input "\n\n")))

(defn preprocess
  "Preprocess input, making it suitable to parse.
  Returns the treated input as string."
  [input]
  (->>
    input
    (remove-comments)
    (trim-each-line)
    (add-padding-newlines)))
